package ru.DmN.bpe.rapi;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.ChunkNibbleArray;
import org.jetbrains.annotations.Nullable;

public interface IChunkLightProvider {
    @Nullable ChunkNibbleArray getLightSection(ChunkSectionPos pos);

    int getLightLevel(BlockPos pos);

    String displaySectionLevel(Vec3i sectionPos);

    void checkBlock(BlockPos pos);

    void resetLevel(Vec3i id);
}
