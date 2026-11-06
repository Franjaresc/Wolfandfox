import { Link, useParams } from 'react-router';
import { apiClient } from '../../api/client';
import { callApi, type ApiFailure } from '../../api/problems';
import { useApiResource } from '../../api/useApiResource';
import { ErrorNotice } from '../../components/ErrorNotice';
import { failureMessage, messages } from '../../messages';
import { RouteClosureSummary } from './RouteClosureSummary';

/** The backend answers 400 to an id that is not a UUID. To a person, that page simply does not exist. */
function isMissing(failure: ApiFailure): boolean {
  return (
    failure.kind === 'problem' &&
    (failure.code === 'NOT_FOUND' || failure.code === 'VALIDATION_FAILED')
  );
}

export function RouteClosureDetailPage() {
  const { id = '' } = useParams();
  const text = messages.closure.detail;

  const { resource, retry } = useApiResource(id, () =>
    callApi(() => apiClient.GET('/api/v1/route-closures/{id}', { params: { path: { id } } })),
  );

  return (
    <section>
      <h2>{text.title}</h2>
      {resource.status === 'loading' ? (
        <p role="status" className="notice">
          {text.loading}
        </p>
      ) : null}
      {resource.status === 'failed' && isMissing(resource.failure) ? (
        <ErrorNotice message={text.notFound} />
      ) : null}
      {resource.status === 'failed' && !isMissing(resource.failure) ? (
        <ErrorNotice message={failureMessage(resource.failure)} onRetry={retry} />
      ) : null}
      {resource.status === 'ready' ? <RouteClosureSummary closure={resource.data} /> : null}
      <Link to="/closures">{text.back}</Link>
    </section>
  );
}
