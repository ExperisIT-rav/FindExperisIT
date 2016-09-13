/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.hod.savedsearches.query;

import com.hp.autonomy.frontend.find.core.savedsearches.EmbeddableIndex;
import com.hp.autonomy.frontend.find.core.savedsearches.FieldTextParser;
import com.hp.autonomy.frontend.find.core.savedsearches.SavedSearchService;
import com.hp.autonomy.frontend.find.core.savedsearches.query.SavedQuery;
import com.hp.autonomy.frontend.find.core.savedsearches.query.SavedQueryController;
import com.hp.autonomy.frontend.find.core.search.QueryRestrictionsBuilderFactory;
import com.hp.autonomy.hod.client.api.resource.ResourceIdentifier;
import com.hp.autonomy.hod.client.api.textindex.query.search.Print;
import com.hp.autonomy.hod.client.error.HodErrorException;
import com.hp.autonomy.searchcomponents.core.search.DocumentsService;
import com.hp.autonomy.searchcomponents.hod.search.HodQueryRestrictions;
import com.hp.autonomy.searchcomponents.hod.search.HodSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HodSavedQueryController extends SavedQueryController<ResourceIdentifier, HodQueryRestrictions, HodSearchResult, HodErrorException> {
    @Autowired
    public HodSavedQueryController(final SavedSearchService<SavedQuery> service,
                                   final DocumentsService<ResourceIdentifier, HodSearchResult, HodErrorException> documentsService,
                                   final FieldTextParser fieldTextParser,
                                   final QueryRestrictionsBuilderFactory<HodQueryRestrictions, ResourceIdentifier> queryRestrictionsBuilderFactory) {
        super(service, documentsService, fieldTextParser, queryRestrictionsBuilderFactory);
    }

    @Override
    protected ResourceIdentifier convertEmbeddableIndex(final EmbeddableIndex embeddableIndex) {
        return new ResourceIdentifier(embeddableIndex.getDomain(), embeddableIndex.getName());
    }

    @Override
    protected String getNoResultsPrintParam() {
        return Print.no_results.name();
    }
}
