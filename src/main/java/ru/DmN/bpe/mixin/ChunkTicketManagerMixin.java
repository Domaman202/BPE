package ru.DmN.bpe.mixin;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.world.SimulationDistanceLevelPropagator;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.*;
import ru.DmN.bpe.rapi.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkTicketManager.class)
public abstract class ChunkTicketManagerMixin implements IChunkTicketManager {
    @Shadow private static int getLevel(SortedArraySet<ChunkTicket<?>> tickets) {return 0;}

    @Shadow private long age;

    @Shadow @Final private SimulationDistanceLevelPropagator simulationDistanceTracker;

    @Shadow @Final public Set<ChunkHolder> chunkHolders;

    @Shadow @Final Executor mainThreadExecutor;

    @Shadow @Final MessageListener<ChunkTaskPrioritySystem.UnblockingMessage> playerTicketThrottlerUnblocker;

    @Shadow @Final private ChunkTicketManager.TicketDistanceLevelPropagator distanceFromTicketTracker;

    @Shadow @Final private ChunkTicketManager.NearbyChunkTicketUpdater nearbyChunkTicketUpdater;

    @Dynamic Map<Vec3i, SortedArraySet<ChunkTicket<?>>> ticketsByPosition;

    @Dynamic  Set<Vec3i> chunkPositions;

    @Dynamic Map<Vec3i, ObjectSet<ServerPlayerEntity>> playersByChunkPos;

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public <T> void addTicketWithLevel(ChunkTicketType<T> type, ChunkPos pos, int level, T argument) {
        this.addTicket(new Vec3i(pos.x, 0, pos.z), new ChunkTicket(type, level, argument));
    }

    @Override
    public void addTicket(Vec3i position, ChunkTicket<?> ticket)  {
        SortedArraySet<ChunkTicket<?>> sortedArraySet = this.getTicketSet(position);
        int i = getLevel(sortedArraySet);
        sortedArraySet.addAndGet(ticket).setTickCreated(this.age);
        if (ticket.getLevel() < i) {
            ((IChunkPosDistanceLevelPropagator) this.distanceFromTicketTracker).updateLevel(position, ticket.getLevel(), true);
        }
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public <T> void addTicket(ChunkTicketType<T> type, ChunkPos pos, int radius, T argument) {
        var chunkTicket = new ChunkTicket<>(type, 33 - radius, argument);
        this.addTicket(new Vec3i(pos.x, 0, pos.z), chunkTicket);
        ((ISimulationDistanceLevelPropagator) this.simulationDistanceTracker).add(new Vec3i(pos.x, 0, pos.z), chunkTicket);
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public boolean shouldDelayShutdown() {
        return !ticketsByPosition.isEmpty();
    }

    @Override
    public SortedArraySet<ChunkTicket<?>> getTicketSet(Vec3i position) {
        return ticketsByPosition.computeIfAbsent(position, (pos) -> SortedArraySet.create(4));
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public boolean tick(ThreadedAnvilChunkStorage chunkStorage) throws Throwable {
        distanceFromNearestPlayerTracker$updateLevels.invoke(ChunkTicketManager.class.getDeclaredField("distanceFromNearestPlayerTracker").get(this));
        this.simulationDistanceTracker.updateLevels();
        nearbyChunkTicketUpdater$updateLevels.invoke(this.nearbyChunkTicketUpdater);
        int i = Integer.MAX_VALUE - this.distanceFromTicketTracker.update(Integer.MAX_VALUE);
        boolean bl = i != 0;

        if (!this.chunkHolders.isEmpty()) {
            this.chunkHolders.forEach((holder) -> holder.tick(chunkStorage, this.mainThreadExecutor));
            this.chunkHolders.clear();
            return true;
        } else {
            if (!chunkPositions.isEmpty()) {

                for (Vec3i l : chunkPositions) {
                    if (this.getTicketSet(l).stream().anyMatch((ticket) -> ticket.getType() == ChunkTicketType.PLAYER)) {
                        ChunkHolder chunkHolder = ((IThreadedAnvilChunkStorage) chunkStorage).getCurrentChunkHolder(l);
                        if (chunkHolder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>> completableFuture = chunkHolder.getEntityTickingFuture();
                        completableFuture.thenAccept((either) -> this.mainThreadExecutor.execute(() -> this.playerTicketThrottlerUnblocker.send(Utils.createUnblockingMessage(() -> {}, l, false))));
                    }
                }

                chunkPositions.clear();
            }

            return bl;
        }
    }

    @Override
    public boolean isUnloaded(Vec3i pos) {
        return false;
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public void removePersistentTickets() {
        ImmutableSet<ChunkTicketType<?>> immutableSet = ImmutableSet.of(ChunkTicketType.UNKNOWN, ChunkTicketType.POST_TELEPORT, ChunkTicketType.LIGHT);
        var objectIterator = this.ticketsByPosition.entrySet().iterator();

        while(objectIterator.hasNext()) {
            var entry = objectIterator.next();
            var iterator = entry.getValue().iterator();
            boolean bl = false;

            while(iterator.hasNext()) {
                ChunkTicket<?> chunkTicket = iterator.next();
                if (!immutableSet.contains(chunkTicket.getType())) {
                    iterator.remove();
                    bl = true;
                    ((ISimulationDistanceLevelPropagator) this.simulationDistanceTracker).remove(entry.getKey(), chunkTicket);
                }
            }

            if (bl) {
                ((ILevelPropagator) this.distanceFromTicketTracker).updateLevel(entry.getKey(), getLevel(entry.getValue()), false);
            }

            if (entry.getValue().isEmpty()) {
                objectIterator.remove();
            }
        }
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public void purge() {
        ++this.age;
        var objectIterator = this.ticketsByPosition.entrySet().iterator();

        while(objectIterator.hasNext()) {
            var entry = objectIterator.next();
            var iterator = entry.getValue().iterator();
            boolean bl = false;

            while(iterator.hasNext()) {
                ChunkTicket<?> chunkTicket = iterator.next();
                if (chunkTicket.isExpired(this.age)) {
                    iterator.remove();
                    bl = true;
                    ((ISimulationDistanceLevelPropagator) this.simulationDistanceTracker).remove(entry.getKey(), chunkTicket);
                }
            }

            if (bl) {
                ((ILevelPropagator) this.distanceFromTicketTracker).updateLevel(entry.getKey(), getLevel(entry.getValue()), false);
            }

            if (entry.getValue().isEmpty()) {
                objectIterator.remove();
            }
        }
    }

    private static final MethodHandle distanceFromNearestPlayerTracker$updateLevels;
    private static final MethodHandle nearbyChunkTicketUpdater$updateLevels;

    static {
        try {
            distanceFromNearestPlayerTracker$updateLevels = MethodHandles.lookup().findVirtual(Class.forName("net.minecraft.server.world.ChunkTicketManager$DistanceFromNearestPlayerTracker"), "updateLevels", MethodType.methodType(void.class));
            nearbyChunkTicketUpdater$updateLevels = MethodHandles.lookup().findVirtual(Class.forName("net.minecraft.server.world.ChunkTicketManager$NearbyChunkTicketUpdater"), "updateLevels", MethodType.methodType(void.class));
        } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
