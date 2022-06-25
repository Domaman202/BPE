package ru.DmN.bpe.rapi;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import org.jetbrains.annotations.Nullable;

public interface ILightStorage {
    boolean hasSection(Vec3i sectionPos);

    @Nullable ChunkNibbleArray getLightSection(Vec3i sectionPos);

    @Nullable ChunkNibbleArray getLightSection(Vec3i sectionPos, boolean cached);

    @Nullable ChunkNibbleArray getLightSection(ChunkToNibbleArrayMap<?> storage, Vec3i sectionPos);

    void enqueueSectionData(Vec3i sectionPos, @Nullable ChunkNibbleArray array, boolean nonEdge);

    int getLight(BlockPos blockPos);

    int getLevel(Vec3i id);

    void updateAll();
}
