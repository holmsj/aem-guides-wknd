package com.adobe.aem.guides.wknd.core.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.keys.AbstractCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;

@Component(
    configurationPolicy = ConfigurationPolicy.REQUIRE, 
    service = CacheKeyFactory.class,
    property =  {
            Constants.SERVICE_RANKING + ":Integer=" + Integer.MAX_VALUE,
            "webconsole.configurationFactory.nameHint=Config name: [ config.configName ]"
    }
)
@Designate(
    ocd = WkndCacheKeyFactory.class,
    factory = true
)
public class WkndCacheKeyFactory implements CacheKeyFactory {

    @ObjectClassDefinition(
        name = "ACS AEM Commons - HTTP Cache - WkndCacheKeyFactory.",
        description = "WkndCacheKeyFactory")
    public @interface Config {
        
        @AttributeDefinition(name = "Config Name")
        String configName() default StringUtils.EMPTY;
    }

    class WkndCacheKey extends AbstractCacheKey implements CacheKey, Serializable {
        private String userId;

        public WkndCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
            super(request, cacheConfig);
            this.userId = request.getAttribute("org.osgi.service.http.authentication.remote.user").toString();
        }

        public WkndCacheKey(String uri, HttpCacheConfig cacheConfig) {
            super(uri, cacheConfig);
            this.userId = StringUtils.EMPTY;
        }

        protected void writeObject(ObjectOutputStream o) throws IOException {
            super.parentWriteObject(o);
            o.writeObject(userId);
        }

        protected void readObject(ObjectInputStream o) throws IOException, ClassNotFoundException {
            super.parentReadObject(o);
            this.userId = (String) o.readObject();
        }

        @Override 
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }

            if (o == null) {
                return false;
            }

            WkndCacheKey that = (WkndCacheKey) o;
            return new EqualsBuilder()
                    .append(getUri(), that.getUri())
                    .append(getResourcePath(), that.getResourcePath())
                    .append(getUserId(), that.getUserId())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(getUri())
                    .append(getUserId()).toHashCode();
        }

        public String getUserId() {
            return userId;
        }
    }

    @Override
    public CacheKey build(SlingHttpServletRequest request, HttpCacheConfig cacheConfig){
        return new WkndCacheKey(request, cacheConfig);
    }

    @Override
    public CacheKey build(String resourcePath, HttpCacheConfig cacheConfig){
        return new WkndCacheKey(resourcePath, cacheConfig);
    }

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig){
        // Check if key is instance of WkndCacheKeyz.
        if (!(key instanceof WkndCacheKey)) {
            return false;
        }
        // Validate if key request uri can be constructed out of uri patterns in cache config.
        return new WkndCacheKey(key.getUri(), cacheConfig).equals(key);
    }
}