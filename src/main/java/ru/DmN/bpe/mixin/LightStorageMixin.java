package ru.DmN.bpe.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import net.minecraft.world.chunk.light.LightStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.DmN.bpe.rapi.ILightStorage;

import java.util.Map;
import java.util.Set;

@Mixin(LightStorage.class)
public abstract class LightStorageMixin implements ILightStorage {
    @Shadow @Final protected ChunkToNibbleArrayMap<?> storage;

    @Shadow protected volatile ChunkToNibbleArrayMap<?> uncachedStorage;

//    @Shadow @Final protected Long2ObjectMap<ChunkNibbleArray> queuedSections;
    protected Map<Vec3i, ChunkNibbleArray> queuedSections;

//    @Shadow @Final private LongSet queuedEdgeSections;
    private Set<Vec3i> queuedEdgeSections;

    @Override
    public boolean hasSection(Vec3i sectionPos) {
        return this.getLightSection(sectionPos, true) != null;
    }

    @Override
    @Nullable
    public ChunkNibbleArray getLightSection(Vec3i sectionPos) {
        ChunkNibbleArray chunkNibbleArray = this.queuedSections.get(sectionPos);
        return chunkNibbleArray != null ? chunkNibbleArray : this.getLightSection(sectionPos, false);
    }

    @Override
    @Nullable
    public ChunkNibbleArray getLightSection(Vec3i sectionPos, boolean cached) {
        return this.getLightSection(cached ? this.storage : this.uncachedStorage, sectionPos);
    }

    @Override
    @Nullable
    public ChunkNibbleArray getLightSection(ChunkToNibbleArrayMap<?> storage, Vec3i sectionPos) {
//        return storage.get(sectionPos);
        return null;
    }

    @Override
    public void enqueueSectionData(Vec3i sectionPos, @Nullable ChunkNibbleArray array, boolean nonEdge) {
        if (array != null) {
            this.queuedSections.put(sectionPos, array);
            if (!nonEdge) {
                this.queuedEdgeSections.add(sectionPos);
            }
        } else {
            this.queuedSections.remove(sectionPos);
        }
    }

//    protected int getLevel(Vec3i id) {
//         if (this.readySections.contains(id)) {
//            return 0;
//        } else {
//            return !this.sectionsToRemove.contains(id) && this.storage.containsKey(id) ? 1 : 2;
//        }
//    }
}
