package ru.DmN.bpe.rapi;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

public interface IChunkTicketManager  {
    void addTicket(Vec3i position, ChunkTicket<?> ticket) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException;

//    void removeTicket(Vec3i pos, ChunkTicket<?> ticket);

    SortedArraySet<ChunkTicket<?>> getTicketSet(Vec3i position);

    boolean isUnloaded(Vec3i pos);

    ChunkHolder getChunkHolder(Vec3i pos);

    ChunkHolder setLevel(Vec3i pos, int level, ChunkHolder holder, int i);
}
