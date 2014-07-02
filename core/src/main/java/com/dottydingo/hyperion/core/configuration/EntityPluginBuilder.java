package com.dottydingo.hyperion.core.configuration;

import com.dottydingo.hyperion.core.endpoint.HttpMethod;
import com.dottydingo.hyperion.core.key.KeyConverter;
import com.dottydingo.hyperion.core.model.PersistentHistoryEntry;
import com.dottydingo.hyperion.core.model.PersistentObject;
import com.dottydingo.hyperion.core.persistence.*;
import com.dottydingo.hyperion.core.persistence.dao.Dao;
import com.dottydingo.hyperion.core.persistence.event.EntityChangeListener;
import com.dottydingo.hyperion.core.persistence.event.PersistentChangeListener;
import com.dottydingo.hyperion.core.registry.*;

import java.util.*;

/**
 * Convenience class for building an entity plugin
 */
public class EntityPluginBuilder
{
    protected String endpointName;
    protected Class<? extends PersistentObject> entityClass;
    protected KeyConverter keyConverter;

    protected HttpMethod[] limitMethods;
    protected int cacheMaxAge = 0;

    protected PersistenceOperations persistenceOperations;
    protected Dao dao;
    protected PersistenceFilter<PersistentObject> persistenceFilter;

    protected Boolean historyEnabled;
    protected Class<? extends PersistentHistoryEntry> historyType;

    protected CreateKeyProcessor defaultCreateKeyProcessor;

    protected List<PersistentChangeListener> persistentChangeListeners = Collections.emptyList();
    protected List<EntityChangeListener> entityChangeListeners = Collections.emptyList();

    protected Map<String,EntitySortBuilder> sortBuilders = Collections.emptyMap();
    protected Map<String,EntityQueryBuilder> queryBuilders = Collections.emptyMap();

    protected List<ApiVersionPluginBuilder> versions;


    public EntityPlugin build(ServiceRegistryBuilder serviceRegistryBuilder) throws Exception
    {
        if(keyConverter == null)
            keyConverter = serviceRegistryBuilder.getDefaultKeyConverter();

        if(persistenceOperations == null)
            serviceRegistryBuilder.getDefaultPersistenceOperations();

        if(dao == null)
            dao = serviceRegistryBuilder.getDefaultDao();

        if(historyEnabled == null)
            historyEnabled = serviceRegistryBuilder.getDefaultHistoryEnabled();

        if(historyType == null)
            historyType = serviceRegistryBuilder.getDefaultHistoryType();

        validateRequired();

        EntityPlugin entityPlugin = new EntityPlugin();
        entityPlugin.setEndpointName(endpointName);
        entityPlugin.setEntityClass(entityClass);
        entityPlugin.setKeyConverter(getKeyConverter(keyConverter));
        if(entityPlugin.getKeyConverter() == null)
            throw new RuntimeException("keyConverter must be specified");

        if(limitMethods != null && limitMethods.length > 0)
            entityPlugin.setLimitMethods(new HashSet<HttpMethod>(Arrays.asList(limitMethods)));

        entityPlugin.setCacheMaxAge(cacheMaxAge);

        entityPlugin.setPersistenceOperations(persistenceOperations);
        entityPlugin.setDao(dao);
        entityPlugin.setPersistenceFilter(getPersistenceFilter(persistenceFilter));


        if(historyEnabled != null)
            entityPlugin.setHistoryEnabled(historyEnabled);
        entityPlugin.setHistoryType(historyType);

        List<PersistentChangeListener> persistentListeners = new ArrayList<>();
        persistentListeners.addAll(serviceRegistryBuilder.getPersistentChangeListeners());
        persistentListeners.addAll(persistentChangeListeners);
        entityPlugin.setPersistentChangeListeners(persistentListeners);

        List<EntityChangeListener> entityListeners = new ArrayList<>();
        entityListeners.addAll(serviceRegistryBuilder.getEntityChangeListeners());
        entityListeners.addAll(entityChangeListeners);
        entityPlugin.setEntityChangeListeners(entityListeners);


        List<ApiVersionPlugin> apiVersionPlugins = new ArrayList<ApiVersionPlugin>();
        for (ApiVersionPluginBuilder versionBuilder : versions)
        {
            apiVersionPlugins.add(versionBuilder.build(this));
        }
        ApiVersionRegistry apiVersionRegistry = new ApiVersionRegistry();
        apiVersionRegistry.setPlugins(apiVersionPlugins);
        entityPlugin.setApiVersionRegistry(apiVersionRegistry);

        return entityPlugin;
    }

    protected void validateRequired()
    {
        if(endpointName == null)
            throw new RuntimeException("endpointName must be specified");

        if(entityClass == null)
            throw new RuntimeException("entityClass must be specified");

        if(persistenceOperations == null)
            throw new RuntimeException("persistenceOperations must be specified");

        if(dao == null)
            throw new RuntimeException("dao must be specified");

        if(versions == null || versions.size() == 0)
            throw new RuntimeException("versions must be specified");

        if(historyEnabled != null && historyEnabled && historyType == null)
            throw new RuntimeException("historyType must be specified when history is enabled.");

    }

    protected KeyConverter getKeyConverter(KeyConverter keyConverter)
    {
        return keyConverter;
    }

    protected PersistenceFilter getPersistenceFilter(PersistenceFilter persistenceFilter)
    {
        if(persistenceFilter == null)
            return new EmptyPersistenceFilter();

        return persistenceFilter;
    }

    protected String getEndpointName()
    {
        return endpointName;
    }

    /**
     * Set the endpoint name for the entity
     * @param endpointName the endpoint name
     */
    public void setEndpointName(String endpointName)
    {
        this.endpointName = endpointName;
    }

    protected Class<? extends PersistentObject> getEntityClass()
    {
        return entityClass;
    }

    /**
     * Set the entity persistent object type
     * @param entityClass the entity persistent object type
     */
    public void setEntityClass(Class<? extends PersistentObject> entityClass)
    {
        this.entityClass = entityClass;
    }

    protected KeyConverter getKeyConverter()
    {
        return keyConverter;
    }

    /**
     * Set the key converter for the entity
     * @param keyConverter the key converter
     */
    public void setKeyConverter(KeyConverter keyConverter)
    {
        this.keyConverter = keyConverter;
    }

    protected HttpMethod[] getLimitMethods()
    {
        return limitMethods;
    }

    /**
     * Set the methods that will be allowed on the endpoint. No setting this enables all request methods.
     * @param limitMethods The methods
     */
    public void setLimitMethods(HttpMethod[] limitMethods)
    {
        this.limitMethods = limitMethods;
    }

    protected int getCacheMaxAge()
    {
        return cacheMaxAge;
    }

    /**
     * Set the maximum cache age to set for this entity. Defaults to 0.
     * @param cacheMaxAge the maximum cache age
     */
    public void setCacheMaxAge(int cacheMaxAge)
    {
        this.cacheMaxAge = cacheMaxAge;
    }

    protected PersistenceOperations getPersistenceOperations()
    {
        return persistenceOperations;
    }

    /**
     * Set the persistence operations instance to use for this entity
     * @param persistenceOperations the persistence operations instance
     */
    public void setPersistenceOperations(PersistenceOperations persistenceOperations)
    {
        this.persistenceOperations = persistenceOperations;
    }

    protected Dao getDao()
    {
        return dao;
    }

    /**
     * Set the Dao to use for this entity
     * @param dao the dao
     */
    public void setDao(Dao dao)
    {
        this.dao = dao;
    }

    protected PersistenceFilter<PersistentObject> getPersistenceFilter()
    {
        return persistenceFilter;
    }

    /**
     * Set the persistence filter to use for this entity
     * @param persistenceFilter the persistence filter
     */
    public void setPersistenceFilter(PersistenceFilter<PersistentObject> persistenceFilter)
    {
        this.persistenceFilter = persistenceFilter;
    }

    protected CreateKeyProcessor getDefaultCreateKeyProcessor()
    {
        return defaultCreateKeyProcessor;
    }

    /**
     * Set the default create key processor to use for versions of this entity. This instance will be
     * used unless one is specified for a specific version.
     * @param defaultCreateKeyProcessor the create key processor
     */
    public void setDefaultCreateKeyProcessor(CreateKeyProcessor defaultCreateKeyProcessor)
    {
        this.defaultCreateKeyProcessor = defaultCreateKeyProcessor;
    }

    protected List<PersistentChangeListener> getPersistentChangeListeners()
    {
        return persistentChangeListeners;
    }

    /**
     * Set the transactional entity change listeners to use for this entity. These will be added to any
     * persistentChangeListeners specified at the registry level.
     * @param persistentChangeListeners the entity change listeners
     */
    public void setPersistentChangeListeners(List<PersistentChangeListener> persistentChangeListeners)
    {
        this.persistentChangeListeners = persistentChangeListeners;
    }

    protected List<EntityChangeListener> getEntityChangeListeners()
    {
        return entityChangeListeners;
    }

    /**
     * Set the post transaction entity change listeners to use for this entity. These will be added to any
     * persistentChangeListeners specified at the registry level.
     * @param entityChangeListeners the entity change listeners
     */
    public void setEntityChangeListeners(List<EntityChangeListener> entityChangeListeners)
    {
        this.entityChangeListeners = entityChangeListeners;
    }

    protected List<ApiVersionPluginBuilder> getVersions()
    {
        return versions;
    }

    protected Map<String, EntitySortBuilder> getSortBuilders()
    {
        return sortBuilders;
    }

    /**
     * Set the sort builders to be used across all versions of the entity
     * @param sortBuilders the sort builders
     */
    public void setSortBuilders(Map<String, EntitySortBuilder> sortBuilders)
    {
        this.sortBuilders = sortBuilders;
    }

    protected Map<String, EntityQueryBuilder> getQueryBuilders()
    {
        return queryBuilders;
    }

    /**
     * Set the query builders to be used across all versions of the entity
     * @param queryBuilders the query builders
     */
    public void setQueryBuilders(Map<String, EntityQueryBuilder> queryBuilders)
    {
        this.queryBuilders = queryBuilders;
    }

    /**
     * Set the flag indicating if history is enabled for this entity.
     * @param historyEnabled the flag
     */
    public void setHistoryEnabled(Boolean historyEnabled)
    {
        this.historyEnabled = historyEnabled;
    }

    /**
     * Set the history type to use. Must be specified if history is enabled.
     * @param historyType the type
     */
    public void setHistoryType(Class<? extends PersistentHistoryEntry> historyType)
    {
        this.historyType = historyType;
    }

    /**
     * Set the plugin versions
     * @param versions the versions
     */
    public void setVersions(List<ApiVersionPluginBuilder> versions)
    {
        this.versions = versions;
    }


}
