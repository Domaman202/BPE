package ru.DmN.bpe.mixin;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.light.LevelPropagator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.bpe.rapi.DefaultHashMap;
import ru.DmN.bpe.rapi.ILevelPropagator;

import java.util.Set;

import static ru.DmN.bpe.Utils.ChunkPos$MARKER;

@Mixin(LevelPropagator.class)
public abstract class LevelPropagatorMixin implements ILevelPropagator {
    @Shadow @Final private int levelCount;

    @Shadow private volatile boolean hasPendingUpdates;

    @Shadow private int minPendingLevel;

    protected DefaultHashMap<Vec3i, Integer> pendingUpdates;

    @Shadow protected abstract int minLevel(int a, int b);

    private Set<Vec3i>[] pendingIdUpdatesByLevel;

    @Override
    public void resetLevel(Vec3i id) {
        this.updateLevel(id, id, this.levelCount - 1, false);
    }

    @Override
    public void updateLevel(Vec3i id, int level, boolean decrease) {
        this.updateLevel(ChunkPos$MARKER, id, level, decrease);
    }

    @Override
    public void updateLevel(Vec3i sourceId, Vec3i id, int level, boolean decrease) {
        this.updateLevel(sourceId, id, level, this.getLevel(id), (this.pendingUpdates.get(id).byteValue() & 255), decrease);
        this.hasPendingUpdates = this.minPendingLevel < this.levelCount;
    }

    @Override
    public void updateLevel(Vec3i sourceId, Vec3i id, int level, int currentLevel, int pendingLevel, boolean decrease) {
        if (!this.isMarker(id)) {
            level = MathHelper.clamp(level, 0, (this.levelCount - 1));
            currentLevel = MathHelper.clamp(currentLevel, 0, (this.levelCount - 1));
            boolean bl;
            if (pendingLevel == 255) {
                bl = true;
                pendingLevel = currentLevel;
            } else {
                bl = false;
            }

            int i;
            if (decrease) {
                i = Math.min(pendingLevel, level);
            } else {
                i = MathHelper.clamp(this.recalculateLevel(id, sourceId, level), 0, (this.levelCount - 1));
            }

            int j = this.minLevel(currentLevel, pendingLevel);
            if (currentLevel != i) {
                int k = this.minLevel(currentLevel, i);
                if (j != k) {
                    this.removePendingUpdate(id, j, k, false);
                }

                this.addPendingUpdate(id, i, k);
            } else if (!bl) {
                this.removePendingUpdate(id, j, this.levelCount, true);
            }
        }
    }

    @Override
    public void addPendingUpdate(Vec3i id, int level, int targetLevel) {
        this.pendingUpdates.put(id, level);
        this.pendingIdUpdatesByLevel[targetLevel].add(id);
        if (this.minPendingLevel > targetLevel) {
            this.minPendingLevel = targetLevel;
        }
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    public final int applyPendingUpdates(int maxSteps) {
        if (this.minPendingLevel >= this.levelCount) {
            return maxSteps;
        }
        while (this.minPendingLevel < this.levelCount && maxSteps > 0) {
            int j;
            --maxSteps;
            var longLinkedOpenHashSet = this.pendingIdUpdatesByLevel[this.minPendingLevel];
            var l = longLinkedOpenHashSet.stream().findFirst().orElseThrow();
            longLinkedOpenHashSet.remove(l);
            int i = MathHelper.clamp(this.getLevel(l), 0, this.levelCount - 1);
            if (longLinkedOpenHashSet.isEmpty()) {
                this.increaseMinPendingLevel(this.levelCount);
            }
            if ((j = this.pendingUpdates.remove(l) & 0xFF) < i) {
                this.setLevel(l, j);
                this.propagateLevel(l, j, true);
                continue;
            }
            if (j <= i) continue;
            this.addPendingUpdate(l, j, this.minLevel(this.levelCount - 1, j));
            this.setLevel(l, this.levelCount - 1);
            this.propagateLevel(l, i, false);
        }
        this.hasPendingUpdates = this.minPendingLevel < this.levelCount;
        return maxSteps;
    }

    /**
     * @author _
     * @reason _
     */
    @Overwrite
    private void increaseMinPendingLevel(int maxLevel) {
        int i = this.minPendingLevel;
        this.minPendingLevel = maxLevel;

        for(int j = i + 1; j < maxLevel; ++j) {
            if (!this.pendingIdUpdatesByLevel[j].isEmpty()) {
                this.minPendingLevel = j;
                break;
            }
        }
    }

    @Override
    public void removePendingUpdate(Vec3i id) {
        int i = (this.pendingUpdates.get(id).byteValue() & 255);
        if (i != 255) {
            int j = this.getLevel(id);
            int k = this.minLevel(j, i);
            this.removePendingUpdate(id, k, this.levelCount, true);
            this.hasPendingUpdates = this.minPendingLevel < this.levelCount;
        }
    }

    @Override
    public void removePendingUpdate(Vec3i id, int level, int levelCount, boolean removeFully) {
        if (removeFully) {
            this.pendingUpdates.remove(id);
        }

        this.pendingIdUpdatesByLevel[level].remove(id);
        if (this.pendingIdUpdatesByLevel[level].isEmpty() && this.minPendingLevel == level) {
            this.increaseMinPendingLevel(levelCount);
        }

    }
}
