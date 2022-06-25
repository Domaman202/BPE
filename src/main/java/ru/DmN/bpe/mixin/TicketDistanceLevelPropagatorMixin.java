package ru.DmN.bpe.mixin;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.bpe.rapi.IChunkTicketManager;
import ru.DmN.bpe.rapi.ILevelPropagator;

@Mixin(ChunkTicketManager.TicketDistanceLevelPropagator.class)
public abstract class TicketDistanceLevelPropagatorMixin implements ILevelPropagator {
    @Final @Shadow ChunkTicketManager field_18255;

    @Override
    public int getLevel(Vec3i id) {
        if (!((IChunkTicketManager) field_18255).isUnloaded(id)) {
            ChunkHolder chunkHolder = ((IChunkTicketManager) field_18255).getChunkHolder(id);
            if (chunkHolder != null) {
                return chunkHolder.getLevel();
            }
        }

        return ThreadedAnvilChunkStorage.MAX_LEVEL + 1;
    }

    @Override
    public void setLevel(Vec3i id, int level) {
        ChunkHolder chunkHolder = ((IChunkTicketManager) field_18255).getChunkHolder(id);
        int i = chunkHolder == null ? ThreadedAnvilChunkStorage.MAX_LEVEL + 1 : chunkHolder.getLevel();
        if (i != level) {
            chunkHolder = ((IChunkTicketManager) field_18255).setLevel(id, level, chunkHolder, i);
            if (chunkHolder != null) {
                field_18255.chunkHolders.add(chunkHolder);
            }
        }
    }

    @Override
    public boolean isMarker(Vec3i id) {
        return false;
    }
}
