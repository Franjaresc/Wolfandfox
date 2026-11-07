import { formatDate, formatDateTime } from '../../format';
import { messages } from '../../messages';
import type { RouteClosure } from './validation';

export function RouteClosureSummary({ closure }: { closure: RouteClosure }) {
  const labels = messages.closure.fields;
  const rows: [string, string | number][] = [
    [labels.routeCode, closure.routeCode],
    [labels.closureDate, formatDate(closure.closureDate)],
    [labels.vehicleCode, closure.vehicleCode],
    [labels.driverCode, closure.driverCode],
    [labels.packagesDelivered, closure.packagesDelivered],
    [labels.packagesFailed, closure.packagesFailed],
    [labels.packagesReturned, closure.packagesReturned],
    [labels.totalPackages, closure.totalPackages],
    [labels.kmDriven, closure.kmDriven],
    [labels.notes, closure.notes ?? messages.closure.noNotes],
    [labels.closedBy, closure.closedBy],
    [labels.closedAt, formatDateTime(closure.closedAt)],
  ];

  return (
    <dl className="summary">
      {rows.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}
