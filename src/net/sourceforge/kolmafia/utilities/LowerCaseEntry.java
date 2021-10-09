package net.sourceforge.kolmafia.utilities;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import net.java.dev.spellcast.utilities.LockableListModel;

public class LowerCaseEntry<K, V> implements Entry<K, V> {
  private final Entry<K, V> original;
  private final K key;
  private V value;
  private String pairString, lowercase;

  public LowerCaseEntry(final Entry<K, V> original) {
    this.original = original;

    this.key = original.getKey();
    this.value = original.getValue();

    this.pairString = this.value + " (" + this.key + ")";
    this.lowercase = this.value.toString().toLowerCase();
  }

  public LowerCaseEntry(final K key, final V value) {
    this.original = null;

    this.key = key;
    this.value = value;

    this.pairString = this.value + " (" + this.key + ")";
    this.lowercase = this.value.toString().toLowerCase();
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof Entry) {
      Entry<?, ?> entry = (Entry<?, ?>) o;

      return this.key.equals(entry.getKey()) && this.value.equals(entry.getValue());
    }

    return false;
  }

  public K getKey() {
    return this.key;
  }

  public V getValue() {
    return this.value;
  }

  @Override
  public String toString() {
    return this.pairString;
  }

  @Override
  public int hashCode() {
    return this.key.hashCode();
  }

  public V setValue(final V newValue) {
    V returnValue = this.value;
    this.value = newValue;

    if (this.original != null) {
      this.original.setValue(newValue);
    }

    this.pairString = this.value + " (" + this.key + ")";
    this.lowercase = this.value.toString().toLowerCase();

    return returnValue;
  }

  public String getLowerCase() {
    return this.lowercase;
  }

  public static final <K, V> LockableListModel<LowerCaseEntry<K, V>> createListModel(
      final Set<Entry<K, V>> entries) {
    LockableListModel<LowerCaseEntry<K, V>> model = new LockableListModel<>();

    Iterator<Entry<K, V>> it = entries.iterator();
    while (it.hasNext()) {
      model.add(new LowerCaseEntry<>(it.next()));
    }

    return model;
  }
}
