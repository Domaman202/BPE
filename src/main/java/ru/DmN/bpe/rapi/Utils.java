package ru.DmN.bpe.rapi;

import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.util.math.Vec3i;

public class Utils {
    public static ChunkTaskPrioritySystem.UnblockingMessage createUnblockingMessage(Runnable callback, Vec3i pos, boolean removeTask) {
        try {
            var c = ChunkTaskPrioritySystem.UnblockingMessage.class.getConstructor(Runnable.class, Vec3i.class, boolean.class);
            c.setAccessible(true);
            return c.newInstance(callback, pos, removeTask);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
