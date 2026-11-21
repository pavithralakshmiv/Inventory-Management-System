
(async function () {
    await initPage('reports');

    const $ = id => document.getElementById(id);
    let chart = null;

    function query() {
        const qs = new URLSearchParams();

        if ($('fromDate').value) {
            qs.set('from', $('fromDate').value);
        }

        if ($('toDate').value) {
            qs.set('to', $('toDate').value);
        }

        if ($('searchBox').value.trim()) {
            qs.set('q', $('searchBox').value.trim());
        }

        return qs;
    }

    function cell(value) {
        if (typeof value === 'number' && !Number.isInteger(value)) {
            return money(value);
        }

        return esc(value);
    }

    async function generate() {
        if (
            $('fromDate').value &&
            $('toDate').value &&
            $('fromDate').value > $('toDate').value
        ) {
            toast('The From date must be before the To date.', 'error');
            return;
        }

        try {
            const report = await api(
                '/api/reports/' +
                $('reportType').value +
                '?' +
                query().toString()
            );

            $('reportTitle').textContent =
                report.title + ' (' + report.rows.length + ' rows)';

            $('thead').innerHTML =
                '<tr>' +
                report.columns.map(c => '<th>' + esc(c) + '</th>').join('') +
                '</tr>';

            $('tbody').innerHTML = report.rows.length
                ? report.rows.map(r =>
                    '<tr>' +
                    r.map(c =>
                        '<td' +
                        (typeof c === 'number' ? ' class="num"' : '') +
                        '>' +
                        cell(c) +
                        '</td>'
                    ).join('') +
                    '</tr>'
                ).join('')
                : '<tr><td class="empty" colspan="' +
                  report.columns.length +
                  '">No records found</td></tr>';

            if (chart) {
                chart.destroy();
                chart = null;
            }

            const hasChart =
                report.chartLabels.length > 0 &&
                typeof Chart !== 'undefined';

            $('chartBox').classList.toggle('hidden', !hasChart);

            if (hasChart) {
                $('chartTitle').textContent = report.chartTitle;

                chart = new Chart($('reportChart'), {
                    type: 'bar',

                    data: {
                        labels: report.chartLabels,

                        datasets: [{
                            label: report.chartTitle,
                            data: report.chartValues,
                            backgroundColor: '#b8860b'
                        }]
                    },

                    options: {
                        responsive: true,
                        maintainAspectRatio: false,

                        plugins: {
                            legend: {
                                display: false
                            }
                        },

                        scales: {
                            y: {
                                beginAtZero: true
                            }
                        }
                    }
                });
            }

        } catch (e) {
            toast(e.message, 'error');
        }
    }

    function exportAs(format) {
        window.location.href =
            '/api/reports/' +
            $('reportType').value +
            '/export?format=' +
            format +
            '&' +
            query().toString();
    }

    $('btnGenerate').addEventListener('click', generate);

    $('reportType').addEventListener('change', generate);

    $('btnPdf').addEventListener('click', () => {
        exportAs('pdf');
    });

    $('btnExcel').addEventListener('click', () => {
        exportAs('xlsx');
    });

    $('searchBox').addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            generate();
        }
    });

    $('btnClose').addEventListener('click', () => {
        window.location.href = 'dashboard.html';
    });

    generate();

})();
