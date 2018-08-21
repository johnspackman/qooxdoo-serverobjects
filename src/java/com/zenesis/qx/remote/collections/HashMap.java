package com.zenesis.qx.remote.collections;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.zenesis.qx.event.EventListener;
import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.event.EventStore;
import com.zenesis.qx.event.EventVerifiable;
import com.zenesis.qx.event.Eventable;
import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxiedContainerAware;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.ProxyProperty;
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.SerializeConstructorArgs;
import com.zenesis.qx.utils.ArrayUtils;

@Properties(extend="com.zenesis.qx.remote.Map")
public class HashMap<K,V> extends java.util.HashMap<K,V> implements Proxied, ProxiedContainerAware, Eventable, EventVerifiable {
	
	private static final long serialVersionUID = -7803265903489114209L;

	private final int hashCode;
	private KeySet keySet;
	private Values values;
	private EntrySet entrySet;
	
	@SuppressWarnings("unused")
    private Proxied container;
	private ProxyProperty property;
    private final EventStore eventStore = new EventStore(this);

	public HashMap() {
		super();
		hashCode = new Object().hashCode();
	}

	public HashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		hashCode = new Object().hashCode();
	}

	public HashMap(int initialCapacity) {
		super(initialCapacity);
		hashCode = new Object().hashCode();
	}

	public HashMap(Map<? extends K, ? extends V> m) {
		super(m);
		hashCode = new Object().hashCode();
	}

	@Override
    public void setProxiedContainer(Proxied container, ProxyProperty property) {
	    this.container = container;
	    this.property = property;
    }

    @Override
    public boolean supportsEvent(String eventName) {
        return eventName.equals("change");
    }

    @Override
    public void disableEvents() {
        eventStore.disableEvents();
    }

    @Override
    public void enableEvents() {
        eventStore.enableEvents();
    }

    @Override
    public boolean eventsEnabled() {
        return eventStore.eventsEnabled();
    }

    @Override
    public boolean addListener(String eventName, EventListener listener) throws IllegalArgumentException {
        return eventStore.addListener(eventName, listener);
    }

    @Override
    public boolean removeListener(String eventName, EventListener listener) {
        return eventStore.removeListener(eventName, listener);
    }

    @Override
    public boolean hasListener(String eventName, EventListener listener) {
        return eventStore.hasListener(eventName, listener);
    }

    @Override
    public void fireEvent(String eventName) {
        eventStore.fireEvent(eventName);
    }

    @Override
    public void fireDataEvent(String eventName, Object data) {
        eventStore.fireDataEvent(eventName, data);
    }

    @SerializeConstructorArgs
	public void serializeConstructorArgs(JsonGenerator jgen) throws IOException {
		jgen.writeStartArray();
		Object[] arr;
		while (true) {
			try {
				arr = superEntrySet().toArray();
				break;
			} catch(ConcurrentModificationException e) {
				// Nothing
			}
		}
		for (Object obj : arr) {
			Entry<K,V> entry = (Entry)obj;
			jgen.writeStartObject();
			jgen.writeObjectField("key", entry.getKey());
			jgen.writeObjectField("value", entry.getValue());
			jgen.writeEndObject();
		}
		jgen.writeEndArray();
        if (property != null) {
            Class keyClass = property.getPropertyClass().getKeyClass();
            if (keyClass == null)
                keyClass = String.class;
            Class valueClass = property.getPropertyClass().getJavaType();
            jgen.writeBoolean(keyClass != String.class && !Enum.class.isAssignableFrom(keyClass));
            if (Proxied.class.isAssignableFrom(keyClass))
                jgen.writeString(keyClass.getName());
            else
                jgen.writeNull();
            if (Proxied.class.isAssignableFrom(valueClass))
                jgen.writeString(valueClass.getName());
            else
                jgen.writeNull();
            
        }
	}
	
	@Override
	public V put(K key, V value) {
		if (ArrayUtils.same(super.get(key), value))
			return value;
		V result = super.put(key, value);
		fire(new MapChangeData().put(key, value, result));
		return result;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		MapChangeData event = new MapChangeData();
		for (Map.Entry e : m.entrySet()) {
			K key = (K)e.getKey();
			V value = (V)e.getValue();
			V oldValue = this.get(key);
			if (!ArrayUtils.same(value, oldValue))
				event.put(key, value, oldValue);
		}
		super.putAll(m);
		fire(event);
	}

	@Override
	public V remove(Object key) {
		if (containsKey(key)) {
			V result = super.remove(key);
			fire(new MapChangeData().remove(key));
			return result;
		}
		return null;
	}
	
	@Override
	public void clear() {
		MapChangeData event = new MapChangeData();
		for (Object key : super.keySet())
			event.remove(key);
		super.clear();
		fire(event);
	}

	@Override
	public Set<K> keySet() {
		if (keySet == null)
			keySet = new KeySet();
		return keySet;
	}
	
	Set<K> superKeySet() {
		return super.keySet();
	}

	@Override
	public Collection<V> values() {
		if (values == null)
			values = new Values();
		return values;
	}
	
	Collection<V> superValues() {
		return super.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		if (entrySet == null)
			entrySet = new EntrySet();
		return entrySet;
	}
	
	Set<Entry<K, V>> superEntrySet() {
		return super.entrySet();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Fires an event
	 * @param event
	 */
	private void fire(MapChangeData event) {
		if (!event.isEmpty()) {
			EventManager.fireDataEvent(this, "change", event);
			ProxyManager.collectionChanged(this, event);
		}
	}
	
	public static class MapChangeEntry {
		private final Object key;
		private final Object value;
		private final Object oldValue;
		
		public MapChangeEntry(Object key, Object value, Object oldValue) {
			super();
			this.key = key;
			this.value = value;
			this.oldValue = oldValue;
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}

		public Object getOldValue() {
			return oldValue;
		}
	}
	
	public static class MapChangeData extends ChangeData {
		public java.util.ArrayList<MapChangeEntry> put;
		public java.util.ArrayList removed;

		public MapChangeData put(Object key, Object value, Object oldValue) {
			if (put == null)
				put = new java.util.ArrayList(5);
			if (removed != null) {
				Iterator it = removed.iterator();
				while (it.hasNext())
					if (ArrayUtils.same(it.next(), key))
						it.remove();
			}
			put.add(new MapChangeEntry(key, value, oldValue));
			return this;
		}

		public MapChangeData remove(Object key) {
			if (removed == null)
				removed = new java.util.ArrayList(5);
			if (put != null) {
				Iterator<MapChangeEntry> it = put.iterator();
				while (it.hasNext())
					if (ArrayUtils.same(it.next().key, key))
						it.remove();
			}
			removed.add(key);
			return this;
		}

		public java.util.ArrayList<MapChangeEntry> getPut() {
			return put;
		}

		public java.util.ArrayList getRemoved() {
			return removed;
		}
		
		public boolean isEmpty() {
			return (put == null || put.isEmpty()) && (removed == null || removed.isEmpty());
		}
	}

    private abstract class AbstractIterator implements Iterator {
    	protected final Iterator<Entry<K,V>> srcIterator;
    	protected Entry<K,V> last;
    	
		public AbstractIterator() {
			super();
			srcIterator = HashMap.this.superEntrySet().iterator();
		}

		@Override
		public boolean hasNext() {
			return srcIterator.hasNext();
		}

		@Override
		public void remove() {
			srcIterator.remove();
			if (last != null)
				HashMap.this.fire(new MapChangeData().remove(last.getKey()));
		}
    }

    private final class ValueIterator extends AbstractIterator {
		@Override
		public V next() {
			last = srcIterator.next();
			return last != null ? last.getValue() : null;
		}
    }

    private final class KeyIterator extends AbstractIterator {
		@Override
		public K next() {
			last = srcIterator.next();
			return last != null ? last.getKey() : null;
		}
    }

    private final class EntryIterator extends AbstractIterator {
		@Override
		public Entry<K,V> next() {
			last = srcIterator.next();
			return last;
		}
    }

    private final class Values extends AbstractCollection<V> {
    	@Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }
    	@Override
        public int size() {
            return HashMap.this.size();
        }
    	@Override
        public boolean contains(Object o) {
            return containsValue(o);
        }
    	@Override
        public void clear() {
        	HashMap.this.clear();
        }
    }
    
    private final class KeySet extends AbstractSet<K> {
    	@Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }
    	@Override
        public int size() {
            return HashMap.this.size();
        }
    	@Override
        public boolean contains(Object o) {
            return containsKey(o);
        }
    	@Override
        public boolean remove(Object o) {
            return HashMap.this.remove(o) != null;
        }
    	@Override
        public void clear() {
            HashMap.this.clear();
        }
    }
    
    private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    	@Override
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
    	@Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>) o;
            for (Entry<K,V> candidate : superEntrySet())
            	if (candidate != null && candidate.equals(e))
            		return true;
            return false;
        }
    	@Override
        public boolean remove(Object o) {
        	Iterator<Entry<K,V>> it = iterator();
        	while (it.hasNext()) {
        		if (it.next().equals(o)) {
        			it.remove();
        			return true;
        		}
        	}
            return false;
        }
    	@Override
        public int size() {
            return HashMap.this.size();
        }
    	@Override
        public void clear() {
            HashMap.this.clear();
        }
    }

}
