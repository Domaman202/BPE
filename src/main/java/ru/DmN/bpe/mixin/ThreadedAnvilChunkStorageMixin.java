package ru.DmN.bpe.mixin;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.bpe.rapi.IThreadedAnvilChunkStorage;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static net.minecraft.server.world.ThreadedAnvilChunkStorage.MAX_LEVEL;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin extends VersionedChunkStorage implements IThreadedAnvilChunkStorage {
    public ThreadedAnvilChunkStorageMixin(Path directory, DataFixer dataFixer, boolean dsync) {
        super(directory, dataFixer, dsync);
    }

    @Shadow int watchDistance;
    @Shadow @Final private ThreadedAnvilChunkStorage.TicketManager ticketManager;

    @Shadow protected abstract void sendWatchPackets(ServerPlayerEntity player, ChunkPos pos, MutableObject<ChunkDataS2CPacket> packet, boolean oldWithinViewDistance, boolean newWithinViewDistance);

    @Shadow public static boolean isWithinDistance(int x1, int z1, int x2, int z2, int distance) {
        return false;
    }

    @Shadow public abstract List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge);

    @Shadow @Final private ServerLightingProvider lightingProvider;
    private Map<Vec3i, ChunkHolder> chunksToUnload;
    @Shadow @Final private PointOfInterestStorage pointOfInterestStorage;
    @Shadow @Final private Queue<Runnable> unloadTaskQueue;
    @Shadow @Final private ChunkTaskPrioritySystem chunkTaskPrioritySystem;
    @Shadow @Final private ThreadExecutor<Runnable> mainThreadExecutor;

    @Shadow protected abstract boolean save(Chunk chunk);

    @Shadow protected abstract boolean save(ChunkHolder chunkHolder);

    @Shadow private boolean chunkHolderListDirty;

    @Shadow @Final private LongSet loadedChunks;
    @Shadow @Final ServerWorld world;
    @Shadow @Final private WorldGenerationProgressListener worldGenerationProgressListener;
    private Map<Vec3i, Long> chunkToNextSaveTimeMs;
    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract CrashException crash(IllegalStateException exception, String details);

    private HashMap<Vec3i, ChunkHolder> currentChunkHolders;
    private volatile Map<Vec3i, ChunkHolder> chunkHolders;
    public Set<Vec3i> unloadedChunks;

    @Override
    public ChunkHolder setLevel(Vec3i pos, int level, ChunkHolder holder, int i) {
        if (i <= MAX_LEVEL || level <= MAX_LEVEL) {
            if (holder != null) {
                holder.setLevel(level);
            }

            if (holder != null) {
                if (level > MAX_LEVEL) {
                    this.unloadedChunks.add(pos);
                } else {
                    this.unloadedChunks.remove(pos);
                }
            }

            if (level <= MAX_LEVEL && holder == null) {
                holder = this.chunksToUnload.remove(pos);
                if (holder != null) {
                    holder.setLevel(level);
                } else {
                    holder = new ChunkHolder(new ChunkPos(pos.getX(), pos.getZ()), level, this.world, this.lightingProvider, this.chunkTaskPrioritySystem, (ChunkHolder.PlayersWatchingChunkProvider) (Object) this);
                }

                this.currentChunkHolders.put(pos, holder);
                this.chunkHolderListDirty = true;
            }

        }
        return holder;
    }

    @Override
    public ChunkHolder getCurrentChunkHolder(Vec3i pos) {
        return this.currentChunkHolders.get(pos);
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public void setViewDistance(int watchDistance) {
        int i = MathHelper.clamp(watchDistance + 1, 3, 33);
        if (i != this.watchDistance) {
            int j = this.watchDistance;
            this.watchDistance = i;
            this.ticketManager.setWatchDistance(this.watchDistance + 1);

            for (ChunkHolder chunkHolder : this.currentChunkHolders.values()) {
                ChunkPos chunkPos = chunkHolder.getPos();
                var mutableObject = new MutableObject<ChunkDataS2CPacket>();
                this.getPlayersWatchingChunk(chunkPos, false).forEach((player) -> {
                    ChunkSectionPos chunkSectionPos = player.getWatchedSection();
                    boolean bl = isWithinDistance(chunkPos.x, chunkPos.z, chunkSectionPos.getSectionX(), chunkSectionPos.getSectionZ(), j);
                    boolean bl2 = isWithinDistance(chunkPos.x, chunkPos.z, chunkSectionPos.getSectionX(), chunkSectionPos.getSectionZ(), this.watchDistance);
                    this.sendWatchPackets(player, chunkPos, mutableObject, bl, bl2);
                });
            }
        }
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public boolean shouldDelayShutdown() {
        return this.lightingProvider.hasUpdates() || !this.chunksToUnload.isEmpty() || !this.currentChunkHolders.isEmpty() || this.pointOfInterestStorage.hasUnsavedElements() || !this.unloadedChunks.isEmpty() || !this.unloadTaskQueue.isEmpty() || this.chunkTaskPrioritySystem.shouldDelayShutdown() || this.ticketManager.shouldDelayShutdown();
    }

    @Override
    public ChunkHolder getChunkHolder(Vec3i pos) {
        return this.chunkHolders.get(pos);
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public void save(boolean flush) {
        if (flush) {
            var list = this.chunkHolders.values().stream().filter(ChunkHolder::isAccessible).peek(ChunkHolder::updateAccessibleStatus).collect(Collectors.toList());
            MutableBoolean mutableBoolean = new MutableBoolean();

            do {
                mutableBoolean.setFalse();
                list.stream().map((chunkHolder) -> {
                    CompletableFuture completableFuture;
                    do {
                        completableFuture = chunkHolder.getSavingFuture();
                        var var10000 = this.mainThreadExecutor;
                        Objects.requireNonNull(completableFuture);
                        var10000.runTasks(completableFuture::isDone);
                    } while(completableFuture != chunkHolder.getSavingFuture());

                    return (Chunk)completableFuture.join();
                }).filter((chunk) -> {
                    return chunk instanceof ReadOnlyChunk || chunk instanceof WorldChunk;
                }).filter(this::save).forEach((chunk) -> {
                    mutableBoolean.setTrue();
                });
            } while(mutableBoolean.isTrue());

            this.unloadChunks(() -> true);
            this.completeAll();
        } else {
            this.chunkHolders.values().forEach(this::save);
        }
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    private void unloadChunks(BooleanSupplier shouldKeepTicking) {
        var longIterator = this.unloadedChunks.iterator();

        for(int i = 0; longIterator.hasNext() && (shouldKeepTicking.getAsBoolean() || i < 200 || this.unloadedChunks.size() > 2000); longIterator.remove()) {
            var l = longIterator.next();
            ChunkHolder chunkHolder = this.currentChunkHolders.remove(l);
            if (chunkHolder != null) {
                this.chunksToUnload.put(l, chunkHolder);
                this.chunkHolderListDirty = true;
                ++i;
                this.tryUnloadChunk(l, chunkHolder);
            }
        }

        int j = Math.max(0, this.unloadTaskQueue.size() - 2000);

        Runnable runnable;
        while((shouldKeepTicking.getAsBoolean() || j > 0) && (runnable = this.unloadTaskQueue.poll()) != null) {
            --j;
            runnable.run();
        }

        int k = 0;
        var objectIterator = this.chunkHolders.values().iterator();

        while(k < 20 && shouldKeepTicking.getAsBoolean() && objectIterator.hasNext()) {
            if (this.save((ChunkHolder)objectIterator.next())) {
                ++k;
            }
        }
    }

    @Override
    public void tryUnloadChunk(Vec3i pos, ChunkHolder holder) {
        CompletableFuture<Chunk> completableFuture = holder.getSavingFuture();
        Consumer<Chunk> var10001 = (chunk) -> {
            CompletableFuture<Chunk> completableFuture2 = holder.getSavingFuture();
            if (completableFuture2 != completableFuture) {
                this.tryUnloadChunk(pos, holder);
            } else {
                if (this.chunksToUnload.remove(pos, holder) && chunk != null) {
                    if (chunk instanceof WorldChunk) {
                        ((WorldChunk)chunk).setLoadedToWorld(false);
                    }

                    this.save(chunk);
                    if (this.loadedChunks.remove(pos) && chunk instanceof WorldChunk) {
                        WorldChunk worldChunk = (WorldChunk)chunk;
                        this.world.unloadEntities(worldChunk);
                    }

                    this.lightingProvider.updateChunkStatus(chunk.getPos());
                    this.lightingProvider.tick();
                    this.worldGenerationProgressListener.setChunkStatus(chunk.getPos(), null);
                    this.chunkToNextSaveTimeMs.remove(new Vec3i(chunk.getPos().x, 0, chunk.getPos().z));
                }

            }
        };
        Objects.requireNonNull(this.unloadTaskQueue);
        completableFuture.thenAcceptAsync(var10001, this.unloadTaskQueue::add).whenComplete((void_, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to save chunk {}", holder.getPos(), throwable);
            }
        });
    }

//    /**
//     * @author _
//     * @reason _
//     */
//    @Overwrite
//    private CompletableFuture<Either<List<Chunk>, ChunkHolder.Unloaded>> getRegion(ChunkPos centerChunk, int margin, IntFunction<ChunkStatus> distanceToStatus) {
//        List<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> list0 = new ArrayList();
//        List<ChunkHolder> list2 = new ArrayList();
//        int i = centerChunk.x;
//        int j = centerChunk.z;
//
//        for(int k = -margin; k <= margin; ++k) {
//            for(int l = -margin; l <= margin; ++l) {
//                int m = Math.max(Math.abs(l), Math.abs(k));
//                final ChunkPos chunkPos = new ChunkPos(i + l, j + k);
//                ChunkHolder chunkHolder = this.getCurrentChunkHolder(new Vec3i(chunkPos.x, 0, chunkPos.z));
//                if (chunkHolder == null) {
//                    return CompletableFuture.completedFuture(Either.right(new ChunkHolder.Unloaded() {
//                        public String toString() {
//                            return "Unloaded " + chunkPos;
//                        }
//                    }));
//                }
//
//                ChunkStatus chunkStatus = distanceToStatus.apply(m);
//                CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> completableFuture = chunkHolder.getChunkAt(chunkStatus, (ThreadedAnvilChunkStorage) (Object) this);
//                list2.add(chunkHolder);
//                list0.add(completableFuture);
//            }
//        }
//
//        CompletableFuture<List<Either<Chunk, ChunkHolder.Unloaded>>> completableFuture2 = Util.combineSafe(list0);
//        CompletableFuture<Either<List<Chunk>, ChunkHolder.Unloaded>> completableFuture3 = completableFuture2.thenApply((chunks) -> {
//            List<Chunk> list = Lists.newArrayList();
//            final int l = 0;
//
//            for(Iterator var7 = chunks.iterator(); var7.hasNext(); ++l) {
//                final Either<Chunk, ChunkHolder.Unloaded> either = (Either)var7.next();
//                if (either == null) {
//                    throw this.crash(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
//                }
//
//                Optional<Chunk> optional = either.left();
//                if (!optional.isPresent()) {
//                    return Either.right(new ChunkHolder.Unloaded() {
//                        public String toString() {
//                            ChunkPos var10000 = new ChunkPos(i + l % (j * 2 + 1), 0 + l / (j * 2 + 1));
//                            return "Unloaded " + var10000 + " " + either.right().get();
//                        }
//                    });
//                }
//
//                list.add((Chunk)optional.get());
//            }
//
//            return Either.left(list);
//        });
//        Iterator var17 = list2.iterator();
//
//        while(var17.hasNext()) {
//            ChunkHolder chunkHolder2 = (ChunkHolder)var17.next();
//            chunkHolder2.combineSavingFuture("getChunkRangeFuture " + centerChunk + " " + margin, completableFuture3);
//        }
//
//        return completableFuture3;
//    }
}
