/*
 * Copyright 2016 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'backbone',
    'underscore',
    'i18n!find/nls/bundle'
], function(Backbone, _, i18n) {

    var ResultRenderer = function (options) {
        this.config = options.config;
    };

    _.extend(ResultRenderer.prototype, {
        getResult: function(model, isPromotion) {
            var templateData = _.find(this.config, function(templateData) {
                return templateData.predicate.call(this, model, isPromotion);
            });

            return templateData.template({
                i18n: i18n,
                data: templateData.data.call(this, model, isPromotion),
                model: model
            });
        }
    });

    return ResultRenderer;
});
