package ru.DmN.bpe.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ChunkPosDistanceLevelPropagator;
import net.minecraft.world.SimulationDistanceLevelPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.bpe.rapi.DefaultHashMap;
import ru.DmN.bpe.rapi.ILevelPropagator;
import ru.DmN.bpe.rapi.ISimulationDistanceLevelPropagator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(SimulationDistanceLevelPropagator.class)
public abstract class SimulationDistanceLevelPropagatorMixin extends ChunkPosDistanceLevelPropagator implements ISimulationDistanceLevelPropagator {
    protected SimulationDistanceLevelPropagatorMixin(int i, int j, int k) {
        super(i, j, k);
    }

    @Shadow
    protected abstract int getLevel(SortedArraySet<ChunkTicket<?>> ticket);

    protected DefaultHashMap<Vec3i, Integer> levels;
    private Map<Vec3i, SortedArraySet<ChunkTicket<?>>> tickets;

    @Override
    public void setLevel(Vec3i id, int level) {
        if (level > 33) {
            this.levels.remove(id);
        } else {
            this.levels.put(id, level);
        }
    }

    @Override
    public void add(Vec3i pos, ChunkTicket<?> ticket) {
        SortedArraySet<ChunkTicket<?>> sortedArraySet = this.getTickets(pos);
        int i = this.getLevel(sortedArraySet);
        sortedArraySet.add(ticket);
        if (ticket.getLevel() < i) {
            ((ILevelPropagator) this).updateLevel(pos, ticket.getLevel(), true);
        }
    }

    @Override
    public <T> void add(ChunkTicketType<T> type, ChunkPos pos, int level, T argument) {
        this.add(new Vec3i(pos.x, 0, pos.z), new ChunkTicket<>(type, level, argument));
    }

    @Override
    public int getLevel(Vec3i id) {
        return this.levels.get(id);
    }

    @Override
    public SortedArraySet<ChunkTicket<?>> getTickets(Vec3i pos) {
        return this.tickets.computeIfAbsent(pos, (p) -> SortedArraySet.create(4));
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    @SuppressWarnings("unchecked")
    public void updatePlayerTickets(int level) {
        List<Pair<ChunkTicket<?>, Vec3i>> list = new ArrayList<>();

        for (var vec3iSortedArraySetEntry : this.tickets.entrySet()) {
            for (ChunkTicket<?> ticket : vec3iSortedArraySetEntry.getValue()) {
                if (ticket.getType() == ChunkTicketType.PLAYER) {
                    list.add(Pair.of(ticket, vec3iSortedArraySetEntry.getKey()));
                }
            }
        }

        for (var chunkTicketVec3iPair : list) {
            this.remove(chunkTicketVec3iPair.getSecond(), chunkTicketVec3iPair.getFirst());
            ChunkPos chunkPos = new ChunkPos(new BlockPos(chunkTicketVec3iPair.getSecond()));
            this.add((ChunkTicketType<ChunkPos>) chunkTicketVec3iPair.getFirst().getType(), chunkPos, level, chunkPos);
        }
    }

    @Override
    public void remove(Vec3i pos, ChunkTicket<?> ticket) {
        SortedArraySet<ChunkTicket<?>> sortedArraySet = this.getTickets(pos);
        sortedArraySet.remove(ticket);
        if (sortedArraySet.isEmpty()) {
            this.tickets.remove(pos);
        }

        ((ILevelPropagator) this).updateLevel(pos, this.getLevel(sortedArraySet), false);
    }

    @Override
    public int getInitialLevel(Vec3i id) {
        var sortedArraySet = this.tickets.get(id);
        return sortedArraySet != null && !sortedArraySet.isEmpty() ? sortedArraySet.first().getLevel() : Integer.MAX_VALUE;
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public void updateLevels() {
        this.applyPendingUpdates(Integer.MAX_VALUE);
    }
}
