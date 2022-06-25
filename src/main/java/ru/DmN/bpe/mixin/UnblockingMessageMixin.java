package ru.DmN.bpe.mixin;

import net.minecraft.server.world.ChunkTaskPrioritySystem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkTaskPrioritySystem.UnblockingMessage.class)
public class UnblockingMessageMixin {
}
