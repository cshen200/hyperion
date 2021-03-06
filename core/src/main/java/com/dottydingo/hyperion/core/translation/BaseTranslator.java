package com.dottydingo.hyperion.core.translation;

import com.dottydingo.hyperion.api.ApiObject;
import com.dottydingo.hyperion.api.exception.InternalException;
import com.dottydingo.hyperion.core.persistence.PersistenceContext;
import com.dottydingo.hyperion.core.model.PersistentObject;
import com.dottydingo.hyperion.core.endpoint.pipeline.auth.AuthorizationContext;

import java.io.Serializable;
import java.util.*;

/**
 */
public abstract class BaseTranslator<C extends ApiObject,P extends PersistentObject> implements Translator<C,P>
{
    protected TypeMapper clientTypeMapper;
    protected TypeMapper persistentTypeMapper;
    private Map<String,FieldMapper> fieldMapperMap = new HashMap<String, FieldMapper>();



    protected abstract C createClientInstance();
    protected abstract P createPersistentInstance();

    public void init()
    {
        clientTypeMapper = new TypeMapper(createClientInstance().getClass());
        persistentTypeMapper = new TypeMapper(createPersistentInstance().getClass());
        initializeDefaultFieldMappers();
        initializeCustomFieldMappers();
    }

    protected void beforeConvert(ObjectWrapper<C> clientObjectWrapper, ObjectWrapper<P> persistentObjectWrapper,
                                 PersistenceContext context){}

    @Override
    public P convertClient(C client, PersistenceContext context)
    {
        P persistentObject = createPersistentInstance();
        ObjectWrapper<P> persistentObjectWrapper = createPersistentObjectWrapper(persistentObject,context);
        ObjectWrapper<C> clientObjectWrapper = createClientObjectWrapper(client,context);

        beforeConvert(clientObjectWrapper,persistentObjectWrapper,context);

        AuthorizationContext authorizationContext = context.getAuthorizationContext();
        for (FieldMapper mapper : fieldMapperMap.values())
        {
            if(authorizationContext.isWritableOnCreate(client,mapper.getClientFieldName()))
                mapper.convertToPersistent(clientObjectWrapper,persistentObjectWrapper,context);
        }

        afterConvert(clientObjectWrapper,persistentObjectWrapper,context);

        return persistentObject;
    }

    protected void afterConvert(ObjectWrapper<C> clientObjectWrapper, ObjectWrapper<P> persistentObjectWrapper,
                                PersistenceContext context){}

    protected boolean beforeCopy(ObjectWrapper<C> clientObjectWrapper, ObjectWrapper<P> persistentObjectWrapper,
                              PersistenceContext context)
    {
        return false;
    }

    @Override
    public boolean copyClient(C client, P persistent, PersistenceContext context)
    {
        boolean dirty = false;
        ObjectWrapper<P> persistentObjectWrapper = createPersistentObjectWrapper(persistent,context);
        ObjectWrapper<C> clientObjectWrapper = createClientObjectWrapper(client,context);

        dirty = beforeCopy(clientObjectWrapper,persistentObjectWrapper,context);

        AuthorizationContext authorizationContext = context.getAuthorizationContext();
        for (FieldMapper mapper : fieldMapperMap.values())
        {
            if(authorizationContext.isWritableOnUpdate(client, persistent,mapper.getClientFieldName())
                    && mapper.convertToPersistent(clientObjectWrapper,persistentObjectWrapper,context))
            {
                dirty = true;
                if(context.getEntityPlugin().hasListeners())
                    context.addChangedField(context.getEntity(),persistent.getId(),mapper.getClientFieldName());
            }
        }

        if(afterCopy(clientObjectWrapper,persistentObjectWrapper,context))
        {
            dirty = true;
        }

        return dirty;
    }

    protected boolean afterCopy(ObjectWrapper<C> clientObjectWrapper, ObjectWrapper<P> persistentObjectWrapper,
                             PersistenceContext context)
    {
        return false;
    }

    @Override
    public C convertPersistent(P persistent, PersistenceContext context)
    {
        C clientObject = createClientInstance();

        ObjectWrapper<P> persistentObjectWrapper = createPersistentObjectWrapper(persistent,context);
        ObjectWrapper<C> clientObjectWrapper = createClientObjectWrapper(clientObject,context);

        Set<String> requestedFields = context.getRequestedFields();
        AuthorizationContext authorizationContext = context.getAuthorizationContext();

        for (Map.Entry<String, FieldMapper> entry : fieldMapperMap.entrySet())
        {
            if((requestedFields == null || requestedFields.contains(entry.getKey()))
                    && authorizationContext.isReadable(persistent,entry.getKey()))
            {

                entry.getValue().convertToClient(persistentObjectWrapper,clientObjectWrapper,context);
            }
        }

        convertPersistent(clientObject,persistent,context);

        return clientObject;
    }

    protected void convertPersistent(C client, P persistent, PersistenceContext context){}

    @Override
    public List<C> convertPersistent(List<P> persistent, PersistenceContext context)
    {
        List<C> list = new LinkedList<C>();
        for (P p : persistent)
        {
            list.add(convertPersistent(p,context));
        }

        return list;
    }


    @Override
    public <ID extends Serializable> ID convertId(C client, PersistenceContext context)
    {
        // we've already validated that this will be an instance of IdFieldMap
        IdFieldMapper<C,P> idFieldMapper = (IdFieldMapper<C, P>) fieldMapperMap.get("id");
        return idFieldMapper.convertId(createClientObjectWrapper(client,context),context);
    }

    private void initializeDefaultFieldMappers()
    {
        Set<String> clientFields =  clientTypeMapper.getFieldNames();
        for (String clientField : clientFields)
        {
            Class clientType = clientTypeMapper.getFieldType(clientField);
            Class persistentType = persistentTypeMapper.getFieldType(clientField);
            if(persistentType != null && clientType.equals(persistentType))
            {
                if(clientField.equals("id"))
                    fieldMapperMap.put(clientField,new DefaultIdFieldMapper());
                else
                {
                    DefaultFieldMapper fieldMapper = new DefaultFieldMapper(clientField);

                    // use a custom property change evaluator for dates
                    if(Date.class.isAssignableFrom(clientType))
                        fieldMapper.setPropertyChangeEvaluator(new DatePropertyChangeEvaluator());

                    fieldMapperMap.put(clientField, fieldMapper);
                }
            }
        }
    }

    private void initializeCustomFieldMappers()
    {
        List<FieldMapper> mappers = getCustomFieldMappers();
        for (FieldMapper mapper : mappers)
        {
            fieldMapperMap.put(mapper.getClientFieldName(),mapper);
        }

        // verify that we have a valid id field mapper defined
        FieldMapper idMapper = fieldMapperMap.get("id");
        if(idMapper == null)
            throw new InternalException("No mapper defined for the id field.");
        if(!(idMapper instanceof IdFieldMapper))
            throw new InternalException("Mapper for the id field must be an instance of IdFieldMapper");
    }

    protected List<FieldMapper> getCustomFieldMappers()
    {
        return new ArrayList<FieldMapper>();
    }

    protected ObjectWrapper<C> createClientObjectWrapper(C client,  PersistenceContext context)
    {
        return new ObjectWrapper<C>(client, clientTypeMapper);
    }

    protected ObjectWrapper<P> createPersistentObjectWrapper(P persistent, PersistenceContext context)
    {
        return new ObjectWrapper<P>(persistent, persistentTypeMapper);
    }

}
