package ru.DmN.bpe.rapi;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.Vec3i;

public interface ITicketManager {
    boolean isUnloaded(Vec3i pos);

    ChunkHolder getChunkHolder(Vec3i pos);

    ChunkHolder setLevel(Vec3i pos, int level, ChunkHolder holder, int i);;
}
