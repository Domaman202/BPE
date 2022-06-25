package ru.DmN.bpe.mixin;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ChunkPosDistanceLevelPropagator;
import org.spongepowered.asm.mixin.Mixin;
import ru.DmN.bpe.Utils;
import ru.DmN.bpe.rapi.IChunkPosDistanceLevelPropagator;

import static ru.DmN.bpe.Utils.ChunkPos$MARKER;

@Mixin(ChunkPosDistanceLevelPropagator.class)
public abstract class ChunkPosDistanceLevelPropagatorMixin extends LevelPropagatorMixin implements IChunkPosDistanceLevelPropagator {
    private int levelCount;

    @Override
    public void updateLevel(Vec3i chunkPos, int distance, boolean decrease) {
        this.updateLevel(ChunkPos$MARKER, chunkPos, distance, decrease);
    }

    @Override
    public boolean isMarker(Vec3i id) {
        return id == ChunkPos$MARKER;
    }

    @Override
    public int recalculateLevel(Vec3i id, Vec3i excludedId, int maxLevel) {
        int i = maxLevel;
        var id_ = new ChunkPos(id.getX(), id.getZ());
        var excludedId_ = new ChunkPos(excludedId.getX(), excludedId.getZ());

        for(int l = -1; l <= 1; ++l) {
            for(int m = -1; m <= 1; ++m) {
                var n = new ChunkPos(id.getX() + l, id.getZ() + m);
                if (n.equals(id_)) {
                    n = new ChunkPos(1875066, 1875066);
                }

                if (n.equals(excludedId_)) {
                    int o = this.getPropagatedLevel(new Vec3i(n.x, 0, n.z), id, this.getLevel(new Vec3i(n.x, 0, n.z)));
                    if (i > o) {
                        i = o;
                    }

                    if (i == 0) {
                        return i;
                    }
                }
            }
        }

        return i;
    }

    @Override
    public void propagateLevel(Vec3i id, int level, boolean decrease) {
        ChunkPos chunkPos = new ChunkPos(id.getX(), id.getZ());
        int i = chunkPos.x;
        int j = chunkPos.z;

        for(int k = -1; k <= 1; ++k) {
            for(int l = -1; l <= 1; ++l) {
                var m = new Vec3i(i + k, 0, j + l);
                if (!m.equals(id)) {
                    this.propagateLevel(id, m, level, decrease);
                }
            }
        }
    }

    @Override
    public void propagateLevel(Vec3i sourceId, Vec3i targetId, int level, boolean decrease) {
        int i = (this.pendingUpdates.get(targetId).byteValue() & 255);
        int j = MathHelper.clamp(this.getPropagatedLevel(sourceId, targetId, level), 0, this.levelCount - 1);
        if (decrease) {
            this.updateLevel(sourceId, targetId, j, this.getLevel(targetId), i, true);
        } else {
            int k;
            boolean bl;
            if (i == 255) {
                bl = true;
                k = MathHelper.clamp(this.getLevel(targetId), 0, this.levelCount - 1);
            } else {
                k = i;
                bl = false;
            }

            if (j == k) {
                this.updateLevel(sourceId, targetId, this.levelCount - 1, bl ? k : this.getLevel(targetId), i, false);
            }
        }
    }

    @Override
    public int getPropagatedLevel(Vec3i sourceId, Vec3i targetId, int level) {
        return sourceId == ChunkPos$MARKER ? this.getInitialLevel(targetId) : level + 1;
    }
}
