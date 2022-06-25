package ru.DmN.bpe.rapi;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.Vec3i;

public interface IThreadedAnvilChunkStorage {
    ChunkHolder getCurrentChunkHolder(Vec3i pos);

    ChunkHolder getChunkHolder(Vec3i pos);

    void tryUnloadChunk(Vec3i pos, ChunkHolder holder);

    ChunkHolder setLevel(Vec3i pos, int level, ChunkHolder holder, int i);;
}
