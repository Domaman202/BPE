package ru.DmN.bpe.mixin;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.bpe.rapi.IThreadedAnvilChunkStorage;
import ru.DmN.bpe.rapi.ITicketManager;

@Mixin(ThreadedAnvilChunkStorage.TicketManager.class)
public abstract class TicketManagerMixin implements ITicketManager {
    @Final @Shadow ThreadedAnvilChunkStorage field_17443;

    @Override
    public ChunkHolder setLevel(Vec3i pos, int level, ChunkHolder holder, int i) {
        return ((IThreadedAnvilChunkStorage) field_17443).setLevel(pos, level, holder, i);
    }

    @Override
    public ChunkHolder getChunkHolder(Vec3i pos) {
        return ((IThreadedAnvilChunkStorage) field_17443).getCurrentChunkHolder(pos);
    }
}
