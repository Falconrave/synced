if (!whirled) {
    var whirled = {};
}

whirled.addCharts = function () {
    eval(bedrock.include({
            'bedrock.util': ['log'],
            'bedrock.iter': ['each'],
            'bedrock.collections': ['List','Dict', 'Set'],
            'panopticon.chart': ['addChart', 'init', 'StackedBarChart',
                                 'SelfContainedEventChart'],
            'panopticon.ui': ['CheckBoxes', 'RadioButtons'],
        }));

    return function() {
        addChart("economy", "exchange_rate", "Exchange Rate", function () {
            var options = {
                xaxis: { mode: "time", minTickSize: [1, "day"]}
            };
            var chart = new SelfContainedEventChart(
                "DailyExchangeRate", function (ev, collector) {
                    collector.assume("Exchange Rate").add(
                        [ev.date, ev.rate]);
                }, options, "date");
            return chart;
        });

        addEarningsChart("coins", new List([
            [20, "Games"],              // coin rewards for playing games
            [34, "Payouts (Obsolete)"], // not used since 2008
            [40, "Badges"],             // coins from earned badges
            [51, "Payouts"],            // creator payouts for cash items
        ]));

        addEarningsChart("bars", new List([
            [50, "Bars Purchased"],
            [54, "Bling to Bars"],
            [55, "Cashed Out"],
            [57, "Subscription Bars"]
        ]));

        addEarningsChart("bling", new List([
            [51, "Payouts"],            // creator payouts for bling items
        ]));

        function addEarningsChart (currency, actionNames) {
            addChart("economy", "earnings_" + currency, "Earnings (" + currency + ")", function () {
                var actions = new CheckBoxes("Actions", "actions", actionNames);

                function valueExtractor (event, name) {
                    if (currency != event.currency || !actions.has(name)) {
                        return 0;
                    }
                    var earned = event["earned:" + name];
                    return (earned > 0) ? earned : undefined;
                }
                var chart = new StackedBarChart(
                    "DailyTransactions", actionNames, valueExtractor, {controls:[actions]});
                return chart;
            });
        }

        addChart("economy", "purchases", "Purchases", function () {
            var actionNames = new List([
                ["furniture", "Furniture"], ["avatars", "Avatars"], ["pets", "Pets"],
                ["decor", "Decor"], ["toys", "Toys"], ["games", "Games"], ["lp", "Level Packs"],
                ["ip", "Item Packs"],
            ]);
//            var actions = new CheckBoxes("Actions", "actions", actionNames);
//            var currency = new RadioButtons("Currency", "currency", ["coins", "bars", "bling" ]);
            function valueExtractor (event, name) {
//                if (currency.value != event.currency) {
//                    return 0;
//                }
//                if (!actions.has(name)) {
//                    return 0;
//                }
                return event[name] || 0;
            }
            var chart = StackedBarChart(
                "DailyPurchases", actionNames, valueExtractor,
                {controls:[/*actions*//*,currency*/]});
        });

        addChart("funnel", "logins", "Daily Logins", function () {
            var sourceNames = new List([
                ["uniqueGuests", "Flash Guests"],
                ["uniquePlayers", "Flash Registered"],
                ["totalPlayers", "Flash Total"],
                ["uniqueVisitors", "Flash+Web Visitors"]]);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            var group = new RadioButtons("Group", "group", ["web", "embed" ]);
            var options = {
                xaxis: { mode: "time", minTickSize: [1, "hour"]},
                controls: [ sources, group ]
            };
            return new SelfContainedEventChart(
                "DailyLoginCount", function (ev, collector) {
                    if ((group.value == "embed") ^ ev.embed) {
                        return;
                    }
                    sourceNames.each(function (bit) {
                        if (sources.has(bit[0])) {
                            collector.assume(bit[1]).add([ev.timestamp, ev[bit[0]]]);
                        }
                    });
                }, options);
        });

        addChart("funnel", "accounts", "New Accounts (Lines)", function () {
            var sourceNames = new List([
                ["organic", "Organic"],
                ["affiliated", "Affiliated"],
                ["fromAd", "From Ad"],
                ["facebookAd", "Facebook Ad"],
                ["facebookAffiliated", "Facebook Affiliate"]]);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            var options = {
                xaxis: { mode: "time", minTickSize: [1, "hour"]},
                controls: [ sources ]
            };

            return new SelfContainedEventChart(
                "DailyAccountsCreated", function (ev, collector) {
                    sourceNames.each(function (bit) {
                        if (sources.has(bit[0])) {
                            collector.assume(bit[1]).add([ev.date, ev[bit[0]]]);
                        }
                    });
                }, options, "date");
        });

        addChart("funnel", "accounts_stacked", "New Accounts (stacked)", function () {
            var sourceNames = new List([
                ["facebookAffiliated", "Facebook Affiliate"],
                ["fromAd", "From Ad"],
                ["facebookAd", "Facebook Ad"],
                ["affiliated", "Affiliated"],
                ["organic", "Organic"],
            ]);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            function valueExtractor (event, name) {
                return (sources.has(name)) ? (event[name] || 0) : 0;
            }
            var chart = new StackedBarChart(
                "DailyAccountsCreated", sourceNames, valueExtractor, {controls:[sources]});
            chart.extractKey = function (ev) { return ev.date; }
            return chart;
        });

        addChart("funnel", "funnel_lines", "Conversion and Retention (line graph)", function () {
            var sourceNames = new List([
                ["visited", "Visited"],
                ["played", "Played"],
                ["registered", "Registered"],
                ["returned", "Returned"],
                ["retained", "Retained"],
                ["paid", "Paid"],
                ["subscribed", "Subscribed"],
            ]);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            var groups = new CheckBoxes("Group", "group", [
                "GWT/Landing",
                "Web/Broken",
                "Web/Other",
                "GWT/Other",
                "Embed/Mochi",
                "Embed/Kongregate",
                "Embed/?Game",
                "Embed/?Room",
                "Embed/Other",
                "Ad/Other",
                "Other/Other"]);

            var options = {
                controls: [ sources, groups ],
                xaxis: {  mode: "time", minTickSize: [1, "day"]}
            };

            var chart = new SelfContainedEventChart("funnel/date", function (ev, collector) {
                sourceNames.each(function (bit) {
                    if (sources.has(bit[0]) && groups.has(ev.group)) {
                        var list = collector.assume(bit[1]);
                        var sz = list.length;
                        var arr;
                        if (sz > 0 && list.get(sz-1)[0] == ev.date) {
                            arr = list.get(sz-1);
                        } else {
                            arr = [ ev.date, 0 ];
                            list.add(arr);
                        }
                        arr[1] += ev[bit[0]];
                    }
                });
            }, options, "date");
            chart.getEvents = function (eventName, callback) {
                $.getJSON("http://www.whirled.com/json/" + eventName + "?jsoncallback=?",
                          callback);
            };
            return chart;
        });
        addChart("funnel", "entry_vectors", "Entry Vectors", function () {
            var phases = [ "subscribed", "paid", "retained", "returned",
                "registered", "played", "visited" ];

            var sourceNames = new List(phases);
            var sources = new CheckBoxes("Sources", "sources", sourceNames);
            var groups = new CheckBoxes("Group", "group", [
                "GWT/Landing",
                "Web/Broken",
                "Web/Other",
                "GWT/Other",
                "Embed/Mochi",
                "Embed/Kongregate",
                "Embed/?Game",
                "Embed/?Room",
                "Embed/Other",
                "Ad/Other",
                "Other/Other"]);

            var options = {
                controls: [ sources, groups ],
            };

            function valueExtractor (event, name) {
                if (sources.has(name) && groups.has(event.group)) {
                    var cumulation = 0;
                    for (var i = 0; i < phases.length; i ++) {
                        if (phases[i] == name) {
                            return (event[phases[i]] - cumulation) || 0;
                        }
                        cumulation += event[phases[i]];
                    }
                }
                return 0;
            }
            var vectorIx = new Dict();
            var chart = new StackedBarChart("funnel/vector", sourceNames, valueExtractor, options);

            chart.gotData = function (events) {
                events.sort(function(e1, e2) {
                    if (e1.visited == e2.visited) {
                        return 0;
                    }
                    return (e1.visited < e2.visited) ? 1 : -1;
                });
            };

            chart.options.prePlot.push(function () {
                var events = chart.data;
                vectorIx.clear();
                var ix = 0;
                each(events, function (event, idx) {
                    if (groups.has(event.group)) {
                        vectorIx.put(event.vector, ix);
                        ix ++;
                    }
                });

                chart.options.bars.barWidth = 0.9 / vectorIx.size();
            });
            chart.options.xaxis.mode = null;
            chart.options.xaxis.min = -0.1;
            chart.options.xaxis.max = 1.1;

            chart.options.xaxis.ticks = function(axisInfo) {
                return vectorIx.items(function (key, value) {
                    return [ value / vectorIx.size(), key ];
                });
            }
            chart.extractKey = function (ev) {
                return vectorIx.get(ev.vector) / vectorIx.size();
            }
            chart.getEvents = function (eventName, callback) {
                $.getJSON("http://www.whirled.com/json/" + eventName + "?jsoncallback=?",
                          callback);
            };
            return chart;
        });
        init();
    }
}();

