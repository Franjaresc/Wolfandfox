import { useRef, useState, type SyntheticEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { apiClient } from '../../api/client';
import { callApi } from '../../api/problems';
import { useApiResource } from '../../api/useApiResource';
import { ErrorNotice } from '../../components/ErrorNotice';
import { Field } from '../../components/Field';
import { failureMessage, messages } from '../../messages';
import { isValidCode, isValidDate, limits } from './validation';

const PAGE_SIZES = [10, 20, 50, limits.pageSizeMaximum];

interface ListParams {
  date: string;
  routeCode: string;
  page: number;
  size: number;
}

/** The URL can be edited by hand, so anything the contract would reject falls back to a default. */
function parseParams(search: URLSearchParams): ListParams {
  const date = search.get('date') ?? '';
  const routeCode = search.get('routeCode') ?? '';
  const page = Number(search.get('page'));
  const size = Number(search.get('size'));
  return {
    date: isValidDate(date) ? date : '',
    routeCode: isValidCode(routeCode) ? routeCode : '',
    page: Number.isInteger(page) && page >= 0 && page <= limits.integerMaximum ? page : 0,
    size: PAGE_SIZES.includes(size) ? size : limits.pageSizeDefault,
  };
}

function toSearch(params: ListParams): Record<string, string> {
  return {
    ...(params.date === '' ? {} : { date: params.date }),
    ...(params.routeCode === '' ? {} : { routeCode: params.routeCode }),
    ...(params.page === 0 ? {} : { page: String(params.page) }),
    ...(params.size === limits.pageSizeDefault ? {} : { size: String(params.size) }),
  };
}

interface FiltersProps {
  date: string;
  routeCode: string;
  onApply: (filters: { date: string; routeCode: string }) => void;
}

function Filters({ date, routeCode, onApply }: FiltersProps) {
  const text = messages.closure.list;
  const [draftDate, setDraftDate] = useState(date);
  const [draftCode, setDraftCode] = useState(routeCode);
  const [codeError, setCodeError] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  // Keeps the inputs in step with the URL when the user goes back or forward.
  const applied = `${date}|${routeCode}`;
  const [lastApplied, setLastApplied] = useState(applied);
  if (lastApplied !== applied) {
    setLastApplied(applied);
    setDraftDate(date);
    setDraftCode(routeCode);
    setCodeError(false);
  }

  const handleSubmit = (event: SyntheticEvent) => {
    event.preventDefault();
    if (draftCode !== '' && !isValidCode(draftCode)) {
      setCodeError(true);
      const element = formRef.current?.elements.namedItem('routeCode');
      if (element instanceof HTMLElement) {
        element.focus();
      }
      return;
    }
    setCodeError(false);
    onApply({ date: draftDate, routeCode: draftCode });
  };

  return (
    <form
      ref={formRef}
      noValidate
      className="filters"
      aria-label={text.filters}
      onSubmit={handleSubmit}
    >
      <Field
        name="date"
        type="date"
        label={messages.closure.fields.closureDate}
        value={draftDate}
        onChange={setDraftDate}
      />
      <Field
        name="routeCode"
        label={messages.closure.fields.routeCode}
        value={draftCode}
        error={codeError ? messages.closure.errors.routeCode.format : undefined}
        onChange={(value) => {
          setDraftCode(value);
          setCodeError(false);
        }}
      />
      <div className="filter-actions">
        <button type="submit">{text.apply}</button>
        <button
          type="button"
          className="secondary"
          onClick={() => {
            setDraftDate('');
            setDraftCode('');
            setCodeError(false);
            onApply({ date: '', routeCode: '' });
          }}
        >
          {text.clear}
        </button>
      </div>
    </form>
  );
}

export function RouteClosureListPage() {
  const [search, setSearch] = useSearchParams();
  const params = parseParams(search);
  const resultsRef = useRef<HTMLDivElement>(null);
  const text = messages.closure.list;

  const { resource, retry } = useApiResource(JSON.stringify(params), () =>
    callApi(() =>
      apiClient.GET('/api/v1/route-closures', {
        params: {
          query: {
            ...(params.date === '' ? {} : { date: params.date }),
            ...(params.routeCode === '' ? {} : { routeCode: params.routeCode }),
            page: params.page,
            size: params.size,
          },
        },
      }),
    ),
  );

  const goToPage = (page: number) => {
    setSearch(toSearch({ ...params, page }));
    // The buttons are replaced while loading, so focus moves to an element that stays.
    resultsRef.current?.focus();
  };

  const hasFilters = params.date !== '' || params.routeCode !== '';

  return (
    <section>
      <h2>{text.title}</h2>
      <Filters
        date={params.date}
        routeCode={params.routeCode}
        onApply={(filters) => {
          setSearch(toSearch({ ...params, ...filters, page: 0 }));
        }}
      />
      <div className="page-size field">
        <label htmlFor="page-size">{text.pageSize}</label>
        <select
          id="page-size"
          value={params.size}
          onChange={(event) => {
            setSearch(toSearch({ ...params, size: Number(event.target.value), page: 0 }));
          }}
        >
          {PAGE_SIZES.map((size) => (
            <option key={size} value={size}>
              {size}
            </option>
          ))}
        </select>
      </div>
      <div ref={resultsRef} tabIndex={-1} aria-busy={resource.status === 'loading'}>
        {resource.status === 'loading' ? (
          <p role="status" className="notice">
            {text.loading}
          </p>
        ) : null}
        {resource.status === 'failed' ? (
          <ErrorNotice message={failureMessage(resource.failure)} onRetry={retry} />
        ) : null}
        {resource.status === 'ready' && resource.data.items.length === 0 ? (
          <p className="notice">
            {hasFilters || params.page > 0 ? text.noMatches : text.empty}
            {hasFilters || params.page > 0 ? null : (
              <>
                {' '}
                <Link to="/closures/new">{text.emptyAction}</Link>
              </>
            )}
          </p>
        ) : null}
        {resource.status === 'ready' && resource.data.items.length > 0 ? (
          <div className="table-scroll">
            <table>
              <caption className="visually-hidden">{text.title}</caption>
              <thead>
                <tr>
                  <th scope="col">{messages.closure.fields.routeCode}</th>
                  <th scope="col">{messages.closure.fields.closureDate}</th>
                  <th scope="col">{messages.closure.fields.vehicleCode}</th>
                  <th scope="col">{messages.closure.fields.driverCode}</th>
                  <th scope="col" className="number">
                    {messages.closure.fields.totalPackages}
                  </th>
                  <th scope="col" className="number">
                    {messages.closure.fields.kmDriven}
                  </th>
                </tr>
              </thead>
              <tbody>
                {resource.data.items.map((closure) => (
                  <tr key={closure.id}>
                    <td>
                      <Link to={`/closures/${closure.id}`}>{closure.routeCode}</Link>
                    </td>
                    <td>{closure.closureDate}</td>
                    <td>{closure.vehicleCode}</td>
                    <td>{closure.driverCode}</td>
                    <td className="number">{closure.totalPackages}</td>
                    <td className="number">{closure.kmDriven}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        {resource.status === 'ready' && (resource.data.totalPages > 0 || params.page > 0) ? (
          <nav className="pagination" aria-label={text.pagination}>
            <button
              type="button"
              className="secondary"
              disabled={params.page === 0}
              onClick={() => {
                goToPage(params.page - 1);
              }}
            >
              {text.previous}
            </button>
            <span>{text.pageOf(params.page + 1, Math.max(resource.data.totalPages, 1))}</span>
            <button
              type="button"
              className="secondary"
              disabled={params.page + 1 >= resource.data.totalPages}
              onClick={() => {
                goToPage(params.page + 1);
              }}
            >
              {text.next}
            </button>
            <span className="muted">{text.total(resource.data.totalElements)}</span>
          </nav>
        ) : null}
      </div>
    </section>
  );
}
