/*
 * Copyright 2016 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.core.savedsearches.query;

import com.hp.autonomy.frontend.find.core.savedsearches.EmbeddableIndex;
import com.hp.autonomy.frontend.find.core.savedsearches.FieldTextParser;
import com.hp.autonomy.frontend.find.core.savedsearches.SavedSearchService;
import com.hp.autonomy.frontend.find.core.search.QueryRestrictionsBuilderFactory;
import com.hp.autonomy.searchcomponents.core.search.DocumentsService;
import com.hp.autonomy.searchcomponents.core.search.QueryRestrictions;
import com.hp.autonomy.searchcomponents.core.search.SearchRequest;
import com.hp.autonomy.searchcomponents.core.search.SearchResult;
import com.hp.autonomy.types.requests.Documents;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequestMapping(SavedQueryController.PATH)
public abstract class SavedQueryController<S extends Serializable, Q extends QueryRestrictions<S>, D extends SearchResult, E extends Exception> {
    static final String PATH = "/api/bi/saved-query";
    static final String NEW_RESULTS_PATH = "/new-results/";

    private final SavedSearchService<SavedQuery> service;
    private final DocumentsService<S, D, E> documentsService;
    private final FieldTextParser fieldTextParser;
    private final QueryRestrictionsBuilderFactory<Q, S> queryRestrictionsBuilderFactory;

    protected SavedQueryController(final SavedSearchService<SavedQuery> service,
                                   final DocumentsService<S, D, E> documentsService,
                                   final FieldTextParser fieldTextParser, final QueryRestrictionsBuilderFactory<Q, S> queryRestrictionsBuilderFactory) {
        this.service = service;
        this.documentsService = documentsService;
        this.fieldTextParser = fieldTextParser;
        this.queryRestrictionsBuilderFactory = queryRestrictionsBuilderFactory;
    }

    protected abstract S convertEmbeddableIndex(EmbeddableIndex embeddableIndex);

    protected abstract String getNoResultsPrintParam();

    @RequestMapping(method = RequestMethod.GET)
    public Set<SavedQuery> getAll() {
        return service.getAll();
    }

    @RequestMapping(method = RequestMethod.POST)
    public SavedQuery create(
            @RequestBody final SavedQuery query
    ) {
        return service.create(query);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public SavedQuery update(
            @PathVariable("id") final long id,
            @RequestBody final SavedQuery query
    ) {
        return service.update(
                new SavedQuery.Builder(query).setId(id).build()
        );
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public void delete(@SuppressWarnings("MVCPathVariableInspection") @PathVariable("id") final long id) {
        service.deleteById(id);
    }

    @RequestMapping(value = NEW_RESULTS_PATH + "{id}", method = RequestMethod.GET)
    public int checkForNewQueryResults(@SuppressWarnings("MVCPathVariableInspection") @PathVariable("id") final long id) throws E {
        int newResults = 0;

        final SavedQuery savedQuery = service.get(id);
        final DateTime dateDocsLastFetched = savedQuery.getDateDocsLastFetched();
        if (savedQuery.getMaxDate() == null || savedQuery.getMaxDate().isAfter(dateDocsLastFetched)) {
            final QueryRestrictions<S> queryRestrictions = queryRestrictionsBuilderFactory.createBuilder()
                    .setQueryText(savedQuery.getQueryText())
                    .setFieldText(fieldTextParser.toFieldText(savedQuery))
                    .setDatabases(convertEmbeddableIndexes(savedQuery.getIndexes()))
                    .setMinDate(dateDocsLastFetched)
                    .setMinScore(savedQuery.getMinScore())
                    .build();
            final SearchRequest<S> searchRequest = new SearchRequest.Builder<S>()
                    .setQueryRestrictions(queryRestrictions)
                    .setMaxResults(1001)
                    .setPrint(getNoResultsPrintParam())
                    .setQueryType(SearchRequest.QueryType.MODIFIED)
                    .build();

            final Documents<?> searchResults = documentsService.queryTextIndex(searchRequest);
            newResults = searchResults.getTotalResults();
        }

        return newResults;
    }

    private List<S> convertEmbeddableIndexes(final Iterable<EmbeddableIndex> embeddableIndexes) {
        final List<S> indexes = new ArrayList<>(CollectionUtils.size(embeddableIndexes));
        if (embeddableIndexes != null) {
            for (final EmbeddableIndex embeddableIndex : embeddableIndexes) {
                indexes.add(convertEmbeddableIndex(embeddableIndex));
            }
        }

        return indexes;
    }
}
