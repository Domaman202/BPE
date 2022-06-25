package ru.DmN.bpe.rapi;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class DefaultHashMap<K, V> extends HashMap<K, V> implements Cloneable {
    public V dfl;

    public DefaultHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public DefaultHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public DefaultHashMap() {
        super();
    }

    public DefaultHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    @Override
    public V get(Object key) {
        V ret = super.get(key);
        return ret == null ? dfl : ret;
    }

    @Override
    public Map<K, V> clone() {
        return (Map<K, V>) super.clone();
    }

    public void defaultReturnValue(int value) {
        this.dfl = (V) (Integer) value;
    }


    public void defaultReturnValue(V value) {
        this.dfl = value;
    }
}
