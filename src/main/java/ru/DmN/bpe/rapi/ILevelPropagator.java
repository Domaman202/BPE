package ru.DmN.bpe.rapi;

import net.minecraft.util.math.Vec3i;

public interface ILevelPropagator {
    void resetLevel(Vec3i id);

    void updateLevel(Vec3i id, int level, boolean decrease);

    void updateLevel(Vec3i sourceId, Vec3i id, int level, boolean decrease);

    int getLevel(Vec3i id);

    void updateLevel(Vec3i sourceId, Vec3i id, int level, int currentLevel, int pendingLevel, boolean decrease);

    boolean isMarker(Vec3i id);

    int recalculateLevel(Vec3i id, Vec3i excludedId, int maxLevel);

    void removePendingUpdate(Vec3i id, int level, int levelCount, boolean removeFully);

    void addPendingUpdate(Vec3i id, int level, int targetLevel);

    void setLevel(Vec3i id, int level);

    void propagateLevel(Vec3i id, int level, boolean decrease);

    void removePendingUpdate(Vec3i id);

    void propagateLevel(Vec3i sourceId, Vec3i targetId, int level, boolean decrease);
}
