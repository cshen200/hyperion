package com.dottydingo.hyperion.core.translation;

import com.dottydingo.hyperion.core.persistence.PersistenceContext;

/**
 * A field mapper that prevents writing to read only fields
 */
public class ReadOnlyFieldMapper<C,P> extends DefaultFieldMapper<C,P>
{
    public ReadOnlyFieldMapper(String name)
    {
        super(name);
    }

    public ReadOnlyFieldMapper(String clientFieldName, String persistentFieldName,ValueConverter valueConverter)
    {
        super(clientFieldName, persistentFieldName, valueConverter);
    }

    @Override
    public boolean convertToPersistent(ObjectWrapper<C> clientObjectWrapper, ObjectWrapper<P> persistentObjectWrapper, PersistenceContext context)
    {
        return false;
    }
}
