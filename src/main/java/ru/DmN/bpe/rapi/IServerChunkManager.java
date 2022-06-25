package ru.DmN.bpe.rapi;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.Vec3i;

public interface IServerChunkManager {
    ChunkHolder getChunkHolder(Vec3i pos);
}
