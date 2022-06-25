package ru.DmN.bpe.rapi;

import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

public interface ISimulationDistanceLevelPropagator extends IChunkPosDistanceLevelPropagator {
    void add(Vec3i pos, ChunkTicket<?> ticket);

    SortedArraySet<ChunkTicket<?>> getTickets(Vec3i pos);

    void remove(Vec3i pos, ChunkTicket<?> ticket);

    <T> void add(ChunkTicketType<T> type, ChunkPos pos, int level, T argument);

    int getLevel(Vec3i id);
}
