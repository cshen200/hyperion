package com.dottydingo.hyperion.core.endpoint.pipeline.phase;

import com.dottydingo.hyperion.api.exception.BadRequestException;
import com.dottydingo.hyperion.core.endpoint.HyperionContext;
import com.dottydingo.hyperion.core.persistence.event.EntityChangeEvent;
import com.dottydingo.hyperion.core.persistence.event.EntityChangeListener;
import com.dottydingo.hyperion.core.persistence.PersistenceContext;
import com.dottydingo.hyperion.core.registry.EntityPlugin;
import com.dottydingo.service.endpoint.context.EndpointRequest;
import com.dottydingo.service.endpoint.pipeline.AbstractEndpointPhase;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 */
public abstract class BasePersistencePhase extends BaseHyperionPhase
{
    protected Set<String> buildFieldSet(String fields)
    {
        if(fields == null || fields.length() == 0)
            return null;

        String[] split = fields.split(",");
        Set<String> fieldSet = new LinkedHashSet<String>();
        for (String s : split)
        {
            fieldSet.add(s.trim());
        }

        return fieldSet;
    }

    protected Integer getIntegerParameter(String name, EndpointRequest request)
    {
        String value = request.getFirstParameter(name);
        if(value == null)
            return null;

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            throw new BadRequestException(String.format("%s must be an integer.",name));
        }
    }

    protected void processChangeEvents(HyperionContext phaseContext,PersistenceContext persistenceContext)
    {
        EntityPlugin entityPlugin = phaseContext.getEntityPlugin();
        if(!entityPlugin.hasEntityChangeListeners())
            return;

        List<EntityChangeListener> entityChangeListeners = entityPlugin.getEntityChangeListeners();
        for (EntityChangeListener entityChangeListener : entityChangeListeners)
        {
            for (EntityChangeEvent event : persistenceContext.getEntityChangeEvents())
            {
                entityChangeListener.processEntityChange(event);
            }
        }
    }

    protected PersistenceContext buildPersistenceContext(HyperionContext context)
    {
        PersistenceContext persistenceContext = new PersistenceContext();
        persistenceContext.setEntityPlugin(context.getEntityPlugin());
        persistenceContext.setEntity(context.getEntityPlugin().getEndpointName());
        persistenceContext.setHttpMethod(context.getEffectiveMethod());
        persistenceContext.setApiVersionPlugin(context.getVersionPlugin());
        persistenceContext.setUserContext(context.getUserContext());
        persistenceContext.setRequestedFields(buildFieldSet(context.getEndpointRequest().getFirstParameter("fields")));
        persistenceContext.setAuthorizationContext(context.getAuthorizationContext());
        persistenceContext.setLocale(context.getLocal());
        persistenceContext.setMessageSource(messageSource);

        return persistenceContext;
    }

}
