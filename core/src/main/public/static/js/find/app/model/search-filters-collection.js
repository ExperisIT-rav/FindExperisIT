/*
 * Copyright 2016 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'backbone',
    'underscore',
    'moment',
    'find/app/model/dates-filter-model',
    'find/app/util/model-any-changed-attribute-listener',
    'parametric-refinement/prettify-field-name',
    'i18n!find/nls/bundle'
], function(Backbone, _, moment, DatesFilterModel, addChangeListener, prettifyFieldName, i18n) {
    "use strict";

    var DATE_FORMAT = "YYYY-MM-DD HH:mm";
    var SHORT_DATE_FORMAT = "YYYY-MM-DD";
    var DATE_SHORTEN_CUTOFF =  7 * 24 * 3600; // interval in seconds at which date format changes to short

    var FilterType = {
        INDEXES: 'INDEXES',
        MAX_DATE: 'MAX_DATE',
        MIN_DATE: 'MIN_DATE',
        DATE_RANGE: 'DATE_RANGE',
        PARAMETRIC: 'PARAMETRIC'
    };

    var customDatesFilters = [
        {attribute: 'customMinDate', type: FilterType.MIN_DATE},
        {attribute: 'customMaxDate', type: FilterType.MAX_DATE}
    ];

    function getDateFilterText(filterType, dateString) {
        var textPrefixKey = filterType === FilterType.MAX_DATE ? 'app.until' : 'app.from';
        return i18n[textPrefixKey] + ': ' + dateString;
    }

    // Get the filter model id for a given parametric field name
    function parametricFilterId(fieldName) {
        return fieldName;
    }

    function formatDate(autnDate, format) {
        return moment(autnDate * 1000).format(format);
    }

    // Get the display text for the given parametric field name and array of selected parametric values
    function parametricFilterText(field, values, ranges, dataType) {
        var round = function (x) {
            // TODO: implement significant figures---e.g. using toPrecision()---instead of simple rounding
            return Math.round(x * 10) / 10;
        };

        var valueText;

        if (!_.isEmpty(values)) {
            valueText = values.join(', ');
        } else {
            valueText = ranges.map(function (range) {
                //Discard time of day if range greater than 1 week
                // U+2013 = en-dash
                if (dataType === 'numeric') {
                    return round(range[0]) + ' \u2013 ' + round(range[1]);
                } else if (dataType === 'date' && range[1] - range[0] <= DATE_SHORTEN_CUTOFF) {
                    return formatDate(range[0], DATE_FORMAT) + ' \u2013 ' + formatDate(range[1], DATE_FORMAT);
                } else if (dataType === 'date') {
                    return formatDate(range[0], SHORT_DATE_FORMAT) + ' \u2013 ' + formatDate(range[1], SHORT_DATE_FORMAT);
                }
            }).join(', ');
        }

        return prettifyFieldName(field) + ': ' + valueText;
    }

    // Get an array of filter model attributes from the selected parametric values collection
    function extractParametricFilters(selectedParametricValues) {
        return _.map(selectedParametricValues.toFieldsAndValues(), function(data, field) {
            return {
                id: parametricFilterId(field),
                field: field,
                text: parametricFilterText(field, data.values, data.range ? [data.range] : [], data.dataType),
                type: FilterType.PARAMETRIC
            };
        });
    }

     // This collection backs the search filters display view. It monitors the query state models and collections and
    // creates/removes it's own models when they change.
    // When a dates filter model is removed, it updates the appropriate request model attribute with a null value. However,
    // this currently can't be done for the selected databases because the databases view isn't backed by a collection.
    return Backbone.Collection.extend({
        initialize: function(models, options) {
            this.indexesCollection = options.indexesCollection;

            this.datesFilterModel = options.queryState.datesFilterModel;
            this.selectedIndexesCollection = options.queryState.selectedIndexes;
            this.selectedParametricValues = options.queryState.selectedParametricValues;

            this.listenTo(this.selectedParametricValues, 'add remove change', this.updateParametricSelection);
            this.listenTo(this.selectedParametricValues, 'reset', this.resetParametricSelection);
            this.listenTo(this.selectedIndexesCollection, 'reset update', this.updateDatabases);
            this.listenTo(this.datesFilterModel, 'change', this.updateDateFilters);

            this.on('remove', function(model) {
                var type = model.get('type');

                if (type === FilterType.PARAMETRIC) {
                    var field = model.get('field');
                    this.selectedParametricValues.remove(this.selectedParametricValues.where({field: field}));
                } else if (type === FilterType.INDEXES) {
                    this.selectedIndexesCollection.set(this.indexesCollection.toResourceIdentifiers());
                } else if (type === FilterType.DATE_RANGE) {
                    if (this.datesFilterModel.get('dateRange') !== DatesFilterModel.DateRange.CUSTOM) {
                        this.datesFilterModel.set('dateRange', null);
                    }
                } else if (type === FilterType.MAX_DATE) {
                    this.datesFilterModel.set('customMaxDate', null);
                } else if (type === FilterType.MIN_DATE) {
                    this.datesFilterModel.set('customMinDate', null);
                }
            });

            var dateRange = this.datesFilterModel.get('dateRange');

            if (dateRange) {
                if (dateRange === DatesFilterModel.DateRange.CUSTOM) {
                    _.each(customDatesFilters, function(filterData) {
                        var currentValue = this.datesFilterModel.get(filterData.attribute);

                        if (currentValue) {
                            models.push({
                                id: filterData.type,
                                type: filterData.type,
                                text: getDateFilterText(filterData.type, currentValue.format('LLL'))
                            });
                        }
                    }, this);
                } else {
                    models.push({
                        id: FilterType.DATE_RANGE,
                        type: FilterType.DATE_RANGE,
                        text: i18n['search.dates.timeInterval.' + dateRange]
                    });
                }
            }

            if (!this.allIndexesSelected()) {
                models.push({
                    id: FilterType.INDEXES,
                    type: FilterType.INDEXES,
                    text: this.getDatabasesFilterText()
                });
            }

            Array.prototype.push.apply(models, extractParametricFilters(this.selectedParametricValues));
        },

        getDatabasesFilterText: function() {
            var selectedIndexNames = this.selectedIndexesCollection.pluck('name');
            return selectedIndexNames.join(', ');
        },

        allIndexesSelected: function() {
            return this.indexesCollection.length === this.selectedIndexesCollection.length;
        },

        updateDatabases: function() {
            var filterModel = this.get(FilterType.INDEXES);

            if (!this.allIndexesSelected()) {
                var filterText = this.getDatabasesFilterText();

                if (filterModel) {
                    filterModel.set('text', filterText);
                } else {
                    // The databases filter model has equal id and type since only one filter of this type can be present
                    this.add({id: FilterType.INDEXES, type: FilterType.INDEXES, text: filterText});
                }
            } else if (this.contains(filterModel)) {
                this.remove(filterModel);
            }
        },

        // Handles add and remove events from the selected parametric values collection
        updateParametricSelection: function(selectionModel) {
            var field = selectionModel.get('field');
            var id = parametricFilterId(field);
            var modelsForField = this.selectedParametricValues.where({field: field});

            if (modelsForField.length) {
                var values = _.chain(modelsForField).invoke('get', 'value').compact().value();
                var ranges = _.chain(modelsForField).invoke('get', 'range').compact().value();

                this.add({
                    id: id,
                    field: field,
                    text: parametricFilterText(field, values, ranges, selectionModel.get('dataType')),
                    type: FilterType.PARAMETRIC
                }, {
                    // Merge true to overwrite the text for any existing model for this field name
                    merge: true
                });
            } else {
                // this.remove(id) doesn't work when this has been called in response to a different remove event
                this.remove(this.where({id: id}));
            }
        },

        updateDateFilters: function() {
            var dateRange = this.datesFilterModel.get('dateRange');

            if (dateRange) {
                if (dateRange === DatesFilterModel.DateRange.CUSTOM) {
                    // Remove any last <period> date filter
                    this.remove(this.where({id: FilterType.DATE_RANGE}));

                    _.each(customDatesFilters, function(filterData) {
                        var currentValue = this.datesFilterModel.get(filterData.attribute);

                        if (currentValue) {
                            var existingModel = this.get(filterData.type);
                            var filterText = getDateFilterText(filterData.type, currentValue.format('LLL'));

                            if (existingModel) {
                                existingModel.set('text', filterText);
                            } else {
                                this.add({
                                    id: filterData.type,
                                    type: filterData.type,
                                    text: filterText
                                });
                            }
                        } else {
                            this.remove(this.where({id: filterData.type}));
                        }
                    }, this);
                } else {
                    // Remove any custom filters
                    this.remove(this.filter(function(model) {
                        return _.contains([FilterType.MAX_DATE, FilterType.MIN_DATE], model.id);
                    }));

                    var existingDateRangeModel = this.get(FilterType.DATE_RANGE);
                    var filterText = i18n['search.dates.timeInterval.' + dateRange];

                    if (existingDateRangeModel) {
                        existingDateRangeModel.set('text', filterText);
                    } else {
                        this.add({
                            id: FilterType.DATE_RANGE,
                            type: FilterType.DATE_RANGE,
                            text: filterText
                        });
                    }
                }
            } else {
                // No date range selected so remove all date filter models
                this.remove(this.filter(function(model) {
                    return _.contains([FilterType.DATE_RANGE, FilterType.MAX_DATE, FilterType.MIN_DATE], model.id);
                }));
            }
        },

        resetParametricSelection: function() {
            this.remove(this.where({type: FilterType.PARAMETRIC}));
            this.add(extractParametricFilters(this.selectedParametricValues));
        }
    }, {
        FilterType: FilterType
    });

});
