package gg.kembel.dui.core;

@FunctionalInterface
public interface ResourceProvider<K, T> {
  ResourceHandle<T> resolve(K key);
}
