/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.core.search;

import com.hp.autonomy.searchcomponents.core.search.DocumentsService;
import com.hp.autonomy.searchcomponents.core.search.GetContentRequest;
import com.hp.autonomy.searchcomponents.core.search.GetContentRequestIndex;
import com.hp.autonomy.searchcomponents.core.search.QueryRestrictions;
import com.hp.autonomy.searchcomponents.core.search.SearchRequest;
import com.hp.autonomy.searchcomponents.core.search.SearchResult;
import com.hp.autonomy.searchcomponents.core.search.SuggestRequest;
import com.hp.autonomy.types.requests.Documents;
import com.hp.autonomy.types.requests.idol.actions.query.params.PrintParam;
import org.apache.commons.collections4.ListUtils;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.*;

@Controller
@RequestMapping(DocumentsController.SEARCH_PATH)
public abstract class DocumentsController<S extends Serializable, Q extends QueryRestrictions<S>, R extends SearchResult, E extends Exception> {
    public static final String SEARCH_PATH = "/api/public/search";
    public static final String QUERY_PATH = "query-text-index/results";
    static final String PROMOTIONS_PATH = "query-text-index/promotions";
    static final String SIMILAR_DOCUMENTS_PATH = "similar-documents";
    static final String GET_DOCUMENT_CONTENT_PATH = "get-document-content";

    public static final String TEXT_PARAM = "text";
    public static final String RESULTS_START_PARAM = "start";
    public static final String MAX_RESULTS_PARAM = "max_results";
    public static final String SUMMARY_PARAM = "summary";
    public static final String INDEXES_PARAM = "indexes";
    private static final String FIELD_TEXT_PARAM = "field_text";
    private static final String SORT_PARAM = "sort";
    private static final String MIN_DATE_PARAM = "min_date";
    private static final String MAX_DATE_PARAM = "max_date";
    private static final String HIGHLIGHT_PARAM = "highlight";
    private static final String MIN_SCORE_PARAM = "min_score";
    static final String REFERENCE_PARAM = "reference";
    static final String AUTO_CORRECT_PARAM = "auto_correct";
    static final String DATABASE_PARAM = "database";

    public static final int MAX_SUMMARY_CHARACTERS = 250;

    protected final DocumentsService<S, R, E> documentsService;
    private final QueryRestrictionsBuilderFactory<Q, S> queryRestrictionsBuilderFactory;

    protected DocumentsController(final DocumentsService<S, R, E> documentsService, final QueryRestrictionsBuilderFactory<Q, S> queryRestrictionsBuilderFactory) {
        this.documentsService = documentsService;
        this.queryRestrictionsBuilderFactory = queryRestrictionsBuilderFactory;
    }

    protected abstract <T> T throwException(final String message) throws E;

    @SuppressWarnings("MethodWithTooManyParameters")
    @RequestMapping(value = QUERY_PATH, method = RequestMethod.GET)
    @ResponseBody
    public Documents<R> query(
            @RequestParam(TEXT_PARAM) final String text,
            @RequestParam(value = RESULTS_START_PARAM, defaultValue = "1") final int resultsStart,
            @RequestParam(MAX_RESULTS_PARAM) final int maxResults,
            @RequestParam(SUMMARY_PARAM) final String summary,
            @RequestParam(INDEXES_PARAM) final List<S> index,
            @RequestParam(value = FIELD_TEXT_PARAM, defaultValue = "") final String fieldText,
            @RequestParam(value = SORT_PARAM, required = false) final String sort,
            @RequestParam(value = MIN_DATE_PARAM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final DateTime minDate,
            @RequestParam(value = MAX_DATE_PARAM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final DateTime maxDate,
            @RequestParam(value = HIGHLIGHT_PARAM, defaultValue = "true") final boolean highlight,
            @RequestParam(value = MIN_SCORE_PARAM, defaultValue = "0") final int minScore,
            @RequestParam(value = AUTO_CORRECT_PARAM, defaultValue = "true") final boolean autoCorrect
    ) throws E {
        final SearchRequest<S> searchRequest = parseRequestParamsToObject(text, resultsStart, maxResults, summary, index, fieldText, sort, minDate, maxDate, highlight, minScore, autoCorrect);
        Documents<R> results = documentsService.queryTextIndex(searchRequest);

        try {

            String ref = null;
            S ind = null;
            Set<GetContentRequestIndex<S>> getContentRequestIndexSet = new HashSet<GetContentRequestIndex<S>>();
            GetContentRequest<S> getContentRequest = null;
            List<R> partialResultsWithAllFields = null;
            Documents<R> completeResults = null;


            List<R> lr = results.getDocuments();

            for (Iterator<R> it = lr.iterator(); it.hasNext(); ) {
                R document = it.next();
                ref = document.getReference();
                ind = (S) document.getIndex();
                if (!(ref.toLowerCase().endsWith(".docx") || ref.toLowerCase().endsWith(".doc") || ref.toLowerCase().endsWith(".pdf"))) {
                    it.remove();
                } else {
                    getContentRequestIndexSet.add(new GetContentRequestIndex<>(ind, Collections.singleton(ref)));
                }
            }

            getContentRequest = new GetContentRequest<>(getContentRequestIndexSet, PrintParam.All.name());
            partialResultsWithAllFields = documentsService.getDocumentContent(getContentRequest);

            for (Iterator<R> it = partialResultsWithAllFields.iterator(); it.hasNext(); ) {
                R document = it.next();
                ref = document.getReference().toLowerCase();
                if (!(ref.toLowerCase().endsWith(".docx") || ref.toLowerCase().endsWith(".doc") || ref.toLowerCase().endsWith(".pdf"))) {
                    it.remove();
                }
            }

            completeResults = new Documents<R>(partialResultsWithAllFields, partialResultsWithAllFields.size(), results.getExpandedQuery(), results.getSuggestion(), results.getAutoCorrection(), results.getWarnings());

            return completeResults;
        } catch (NullPointerException e) {
            return new Documents<R>(new ArrayList<R>(), results.getTotalResults(), results.getExpandedQuery(), results.getSuggestion(), results.getAutoCorrection(), results.getWarnings());

        }
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    @RequestMapping(value = PROMOTIONS_PATH, method = RequestMethod.GET)
    @ResponseBody
    public Documents<R> queryForPromotions(
            @RequestParam(TEXT_PARAM) final String text,
            @RequestParam(value = RESULTS_START_PARAM, defaultValue = "1") final int resultsStart,
            @RequestParam(MAX_RESULTS_PARAM) final int maxResults,
            @RequestParam(SUMMARY_PARAM) final String summary,
            @RequestParam(value = INDEXES_PARAM, required = false) final List<S> index,
            @RequestParam(value = FIELD_TEXT_PARAM, defaultValue = "") final String fieldText,
            @RequestParam(value = SORT_PARAM, required = false) final String sort,
            @RequestParam(value = MIN_DATE_PARAM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final DateTime minDate,
            @RequestParam(value = MAX_DATE_PARAM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final DateTime maxDate,
            @RequestParam(value = HIGHLIGHT_PARAM, defaultValue = "true") final boolean highlight,
            @RequestParam(value = MIN_SCORE_PARAM, defaultValue = "0") final int minScore,
            @RequestParam(value = AUTO_CORRECT_PARAM, defaultValue = "true") final boolean autoCorrect
    ) throws E {
        final SearchRequest<S> searchRequest = parseRequestParamsToObject(text, resultsStart, maxResults, summary, index, fieldText, sort, minDate, maxDate, highlight, minScore, autoCorrect);
        return documentsService.queryTextIndexForPromotions(searchRequest);
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    private SearchRequest<S> parseRequestParamsToObject(final String queryText, final int resultsStart, final int maxResults, final String summary, final List<S> databases, final String fieldText, final String sort, final DateTime minDate, final DateTime maxDate, final boolean highlight, final Integer minScore, final boolean autoCorrect) {
        final QueryRestrictions<S> queryRestrictions = queryRestrictionsBuilderFactory.createBuilder()
                .setQueryText(queryText)
                .setFieldText(fieldText)
                .setDatabases(ListUtils.emptyIfNull(databases))
                .setMinDate(minDate)
                .setMaxDate(maxDate)
                .setMinScore(minScore)
                .build();

        return new SearchRequest.Builder<S>()
                .setQueryRestrictions(queryRestrictions)
                .setStart(resultsStart)
                .setMaxResults(maxResults)
                .setSummary(summary)
                .setSummaryCharacters(MAX_SUMMARY_CHARACTERS)
                .setSort(sort)
                .setHighlight(highlight)
                .setAutoCorrect(autoCorrect)
                .build();
    }

    @SuppressWarnings("MethodWithTooManyParameters")
    @RequestMapping(value = SIMILAR_DOCUMENTS_PATH, method = RequestMethod.GET)
    @ResponseBody
    public Documents<R> findSimilar(
            @RequestParam(REFERENCE_PARAM) final String reference,
            @RequestParam(value = RESULTS_START_PARAM, defaultValue = "1") final int resultsStart,
            @RequestParam(MAX_RESULTS_PARAM) final int maxResults,
            @RequestParam(SUMMARY_PARAM) final String summary,
            @RequestParam(value = INDEXES_PARAM, required = false) final List<S> databases,
            @RequestParam(value = FIELD_TEXT_PARAM, defaultValue = "") final String fieldText,
            @RequestParam(value = SORT_PARAM, required = false) final String sort,
            @RequestParam(value = MIN_DATE_PARAM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final DateTime minDate,
            @RequestParam(value = MAX_DATE_PARAM, required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final DateTime maxDate,
            @RequestParam(value = HIGHLIGHT_PARAM, defaultValue = "true") final boolean highlight,
            @RequestParam(value = MIN_SCORE_PARAM, defaultValue = "0") final int minScore
    ) throws E {
        final QueryRestrictions<S> queryRestrictions = queryRestrictionsBuilderFactory.createBuilder()
                .setFieldText(fieldText)
                .setDatabases(ListUtils.emptyIfNull(databases))
                .setMinDate(minDate)
                .setMaxDate(maxDate)
                .setMinScore(minScore)
                .build();

        final SuggestRequest<S> suggestRequest = new SuggestRequest.Builder<S>()
                .setReference(reference)
                .setQueryRestrictions(queryRestrictions)
                .setStart(resultsStart)
                .setMaxResults(maxResults)
                .setSummary(summary)
                .setSummaryCharacters(MAX_SUMMARY_CHARACTERS)
                .setSort(sort)
                .setHighlight(highlight)
                .build();


        Documents<R> res =  documentsService.findSimilar(suggestRequest);
        String ref = null;

        List<R> lr = res.getDocuments();

        for (Iterator<R> it = lr.iterator(); it.hasNext(); ) {
            R document = it.next();
            ref = document.getReference();
            if (!(ref.toLowerCase().endsWith(".docx") || ref.toLowerCase().endsWith(".doc") || ref.toLowerCase().endsWith(".pdf"))) {
                it.remove();
            }
        }

        System.out.println("count similar end = "+lr.size());

        return new Documents<R>(lr, res.getTotalResults(), res.getExpandedQuery(), res.getSuggestion(), res.getAutoCorrection(), res.getWarnings());
    }

    @RequestMapping(value = GET_DOCUMENT_CONTENT_PATH, method = RequestMethod.GET)
    @ResponseBody
    public R getDocumentContent(
            @RequestParam(REFERENCE_PARAM) final String reference,
            @RequestParam(DATABASE_PARAM) final S database
    ) throws E {
        final GetContentRequestIndex<S> getContentRequestIndex = new GetContentRequestIndex<>(database, Collections.singleton(reference));
        final GetContentRequest<S> getContentRequest = new GetContentRequest<>(Collections.singleton(getContentRequestIndex), PrintParam.All.name());
        final List<R> results = documentsService.getDocumentContent(getContentRequest);

        return results.isEmpty() ? this.<R>throwException("No content found for document with reference " + reference + " in database " + database) : results.get(0);
    }
}
