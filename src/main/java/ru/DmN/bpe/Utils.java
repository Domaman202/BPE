package ru.DmN.bpe;

import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class Utils {
    public static final Vec3i ChunkPos$MARKER = new Vec3i(1875066, 0, 1875066);

    public static MethodNode findMethod(ClassNode clazz, String name, String desc) {
        for (var method : clazz.methods)
            if (method.name.equals(name) && method.desc.equals(desc))
                return method;
        return null;
    }

    public static void removeMethod(ClassNode clazz, String name, String desc) {
        clazz.methods.remove(findMethod(clazz, name, desc));
    }

    public static MethodVisitor replaceMethod(ClassNode clazz, String name, String desc) {
        var method = findMethod(clazz, name, desc);
        clazz.methods.remove(method);
        return clazz.visitMethod(method.access, name, desc, method.signature, method.exceptions.toArray(new String[0]));
    }

    public static FieldNode findField(ClassNode clazz, String name) {
        for (var field : clazz.fields)
            if (field.name.equals(name))
                return field;
        return null;
    }

    public static void removeField(ClassNode clazz, String name) {
        clazz.fields.remove(findField(clazz, name));
    }

    public static void replaceField(ClassNode clazz, String name, String desc, String signature) {
        var f = findField(clazz, name);
        clazz.fields.remove(f);
        clazz.fields.add(new FieldNode(f.access, name, desc, signature, null));
    }

    public static Vec3i ChunkSectionPos$fromBlockPos(Vec3i in) {
        return new Vec3i(ChunkSectionPos.getSectionCoord(in.getX()), ChunkSectionPos.getSectionCoord(in.getY()), ChunkSectionPos.getSectionCoord(in.getZ()));
    }
}
