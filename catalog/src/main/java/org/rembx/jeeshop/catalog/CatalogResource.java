package org.rembx.jeeshop.catalog;


import org.apache.commons.collections.CollectionUtils;
import org.rembx.jeeshop.catalog.model.Catalog;
import org.rembx.jeeshop.catalog.model.CatalogPersistenceUnit;
import org.rembx.jeeshop.catalog.model.Category;
import org.rembx.jeeshop.role.JeeshopRoles;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.rembx.jeeshop.catalog.model.QCatalog.catalog;
import static org.rembx.jeeshop.catalog.model.QCategory.category;
import static org.rembx.jeeshop.role.AuthorizationUtils.isAdminUser;

/**
 * @author remi
 */

@Path("/catalogs")
@Stateless
public class CatalogResource implements Serializable {

    @PersistenceContext(unitName = CatalogPersistenceUnit.NAME)
    private EntityManager entityManager;

    @Inject
    private CatalogItemFinder catalogItemFinder;

    @Resource
    private SessionContext sessionContext;

    public CatalogResource(){

    }

    public CatalogResource(EntityManager entityManager, CatalogItemFinder catalogItemFinder) {
        this.entityManager = entityManager;
        this.catalogItemFinder = catalogItemFinder;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(JeeshopRoles.ADMIN)
    public Catalog modify(Catalog catalog){
        Catalog originalCatalog = entityManager.find(Catalog.class, catalog.getId());
        if (originalCatalog == null){
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        if (CollectionUtils.isEmpty(catalog.getRootCategories())){
            catalog.setRootCategories(originalCatalog.getRootCategories());
        }

        return  entityManager.merge(catalog);
    }


    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(JeeshopRoles.ADMIN)
    public List<Catalog> findAll(@QueryParam("start") Integer start, @QueryParam("size") Integer size) {
        return catalogItemFinder.findAll(catalog, start, size);
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(JeeshopRoles.ADMIN)
    public Long count() {
        return catalogItemFinder.countAll(catalog);
    }

    @GET
    @Path("/{catalogId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({JeeshopRoles.ADMIN, JeeshopRoles.USER})
    public Catalog find(@PathParam("catalogId") @NotNull Long catalogId, @QueryParam("locale") String locale) {
        Catalog catalog = entityManager.find(Catalog.class, catalogId);

        if (isAdminUser(sessionContext))
            return catalog;
        else
            return catalogItemFinder.filterVisible(catalog, locale);
    }

    @GET
    @Path("/{catalogId}/categories")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public List<Category> findCategories(@PathParam("catalogId") @NotNull Long catalogId, @QueryParam("locale") String locale) {

        Catalog catalog = entityManager.find(Catalog.class, catalogId);
        if (catalog == null){
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        List<Category> rootCategories = catalog.getRootCategories();
        if (rootCategories.isEmpty()){
            return new ArrayList<>();
        }

        if (isAdminUser(sessionContext)){
            return rootCategories;
        }else{
            return catalogItemFinder.findVisibleCatalogItems(category, rootCategories, locale);
        }

    }

}
