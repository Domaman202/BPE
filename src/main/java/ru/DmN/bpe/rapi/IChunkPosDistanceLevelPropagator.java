package ru.DmN.bpe.rapi;

import net.minecraft.util.math.Vec3i;

public interface IChunkPosDistanceLevelPropagator extends ILevelPropagator {
    void updateLevel(Vec3i chunkPos, int distance, boolean decrease);

    void updateLevel(Vec3i sourceId, Vec3i id, int level, boolean decrease);

    boolean isMarker(Vec3i id);

    int recalculateLevel(Vec3i id, Vec3i excludedId, int maxLevel);

    int getLevel(Vec3i id);

    int getPropagatedLevel(Vec3i sourceId, Vec3i targetId, int level);

    int getInitialLevel(Vec3i id);
}
