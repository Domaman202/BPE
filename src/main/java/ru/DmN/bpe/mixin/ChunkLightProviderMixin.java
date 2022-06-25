package ru.DmN.bpe.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.bpe.Utils;
import ru.DmN.bpe.rapi.IChunkLightProvider;
import ru.DmN.bpe.rapi.ILightStorage;

@Mixin(ChunkLightProvider.class)
public abstract class ChunkLightProviderMixin extends LevelPropagatorMixin implements IChunkLightProvider {
    @Shadow @Final protected LightStorage<?> lightStorage;

    @Override
    @Nullable
    public ChunkNibbleArray getLightSection(ChunkSectionPos pos) {
        return ((ILightStorage) this.lightStorage).getLightSection(pos);
    }

    @Override
    public int getLightLevel(BlockPos pos) {
        return ((ILightStorage) this.lightStorage).getLight(pos);
    }

    @Override
    public String displaySectionLevel(Vec3i sectionPos) {
        int var10000 = ((ILightStorage) this.lightStorage).getLevel(sectionPos);
        return "" + var10000;
    }

    @Override
    public void checkBlock(BlockPos pos) {
        this.resetLevel(pos);
        for (Direction direction : Direction.values()) {
            this.resetLevel(pos.add(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ()));
        }
    }

    @Override
    public void resetLevel(Vec3i id) {
        ((ILightStorage) this.lightStorage).updateAll();
        if (((ILightStorage) this.lightStorage).hasSection(Utils.ChunkSectionPos$fromBlockPos(id))) {
            super.resetLevel(id);
        }
    }
}
