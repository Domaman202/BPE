package ru.DmN.bpe.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.*;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.bpe.rapi.IServerChunkManager;
import ru.DmN.bpe.rapi.IThreadedAnvilChunkStorage;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin implements IServerChunkManager {
    @Shadow @Final public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

    @Shadow abstract boolean tick();

    @Shadow protected abstract boolean isMissingForLevel(@Nullable ChunkHolder holder, int maxLevel);

    @Shadow @Final private ChunkTicketManager ticketManager;

    @Shadow @Final private ServerWorld world;

    @Override
    public ChunkHolder getChunkHolder(Vec3i pos) {
        return ((IThreadedAnvilChunkStorage) this.threadedAnvilChunkStorage).getChunkHolder(pos);
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    private CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getChunkFuture(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        int i = 33 + ChunkStatus.getDistanceFromFull(leastStatus);
        ChunkHolder chunkHolder = this.getChunkHolder(new Vec3i(chunkX, 0, chunkZ));
        if (create) {
            this.ticketManager.addTicketWithLevel(ChunkTicketType.UNKNOWN, chunkPos, i, chunkPos);
            if (this.isMissingForLevel(chunkHolder, i)) {
                Profiler profiler = this.world.getProfiler();
                profiler.push("chunkLoad");
                this.tick();
                chunkHolder = this.getChunkHolder(new Vec3i(chunkX, 0, chunkZ));
                profiler.pop();
                if (this.isMissingForLevel(chunkHolder, i)) {
                    throw Util.throwOrPause(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            }
        }

        return this.isMissingForLevel(chunkHolder, i) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : chunkHolder.getChunkAt(leastStatus, this.threadedAnvilChunkStorage);
    }
}
