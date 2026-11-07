import { useEffect, useRef, useState, type SyntheticEvent } from 'react';
import { apiClient } from '../../api/client';
import { callApi, type ApiFailure } from '../../api/problems';
import { ErrorNotice } from '../../components/ErrorNotice';
import { Field } from '../../components/Field';
import { failureMessage, messages } from '../../messages';
import { RouteClosureSummary } from './RouteClosureSummary';
import {
  formFields,
  isFormField,
  liveTotal,
  localToday,
  toRequest,
  validateField,
  validateForm,
  type FieldError,
  type FormErrors,
  type FormField,
  type FormValues,
  type RouteClosure,
} from './validation';

const EMPTY_FORM: FormValues = {
  routeCode: '',
  closureDate: '',
  vehicleCode: '',
  driverCode: '',
  packagesDelivered: '',
  packagesFailed: '',
  packagesReturned: '',
  kmDriven: '',
  notes: '',
};

/** The backend's field messages are never shown: a named field gets our text for that field. */
function serverFieldErrors(failure: ApiFailure): FormErrors {
  const errors: FormErrors = {};
  if (failure.kind === 'problem' && failure.code === 'VALIDATION_FAILED') {
    for (const field of failure.fields.filter(isFormField)) {
      errors[field] = 'rejected';
    }
  }
  return errors;
}

export function RouteClosureFormPage() {
  const [values, setValues] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState<FormErrors>({});
  const [failure, setFailure] = useState<ApiFailure | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [created, setCreated] = useState<RouteClosure | null>(null);
  // Caps the date picker. Validation reads the date afresh, in case the page stays open past midnight.
  const [today] = useState(localToday);
  // A new object on every failed submit, so focus moves again even if the same field is wrong.
  const [focusRequest, setFocusRequest] = useState<{ errors: FormErrors } | null>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const alertRef = useRef<HTMLDivElement>(null);
  const confirmationRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    if (focusRequest === null) {
      return;
    }
    const first = formFields.find((field) => focusRequest.errors[field] !== undefined);
    if (first === undefined) {
      alertRef.current?.focus();
      return;
    }
    const element = formRef.current?.elements.namedItem(first);
    if (element instanceof HTMLElement) {
      element.focus();
    }
  }, [focusRequest]);

  useEffect(() => {
    if (created !== null) {
      confirmationRef.current?.focus();
    }
  }, [created]);

  const setField = (field: FormField, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    if (errors[field] !== undefined) {
      setErrors((current) => ({ ...current, [field]: validateField(field, value, localToday()) }));
    }
  };

  const handleSubmit = async (event: SyntheticEvent) => {
    event.preventDefault();
    if (submitting) {
      return;
    }
    setFailure(null);
    const clientErrors = validateForm(values, localToday());
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length > 0) {
      setFocusRequest({ errors: clientErrors });
      return;
    }
    setSubmitting(true);
    const result = await callApi(() =>
      apiClient.POST('/api/v1/route-closures', { body: toRequest(values) }),
    );
    setSubmitting(false);
    if (result.ok) {
      setCreated(result.data);
      return;
    }
    const fieldErrors = serverFieldErrors(result.failure);
    setErrors(fieldErrors);
    setFailure(result.failure);
    setFocusRequest({ errors: fieldErrors });
  };

  const reset = () => {
    setValues(EMPTY_FORM);
    setErrors({});
    setFailure(null);
    setCreated(null);
  };

  const text = messages.closure;

  if (created !== null) {
    return (
      <section>
        <h2 ref={confirmationRef} tabIndex={-1}>
          {text.form.confirmation}
        </h2>
        <RouteClosureSummary closure={created} />
        <button type="button" onClick={reset}>
          {text.form.closeAnother}
        </button>
      </section>
    );
  }

  const fieldProps = (field: FormField) => {
    const error = errors[field];
    const errorTexts: Partial<Record<FieldError, string>> = text.errors[field];
    return {
      name: field,
      label: text.fields[field],
      value: values[field],
      error: error === undefined ? undefined : errorTexts[error],
      onChange: (value: string) => {
        setField(field, value);
      },
    };
  };
  const total = liveTotal(values);

  return (
    <section>
      <h2>{text.form.title}</h2>
      <p className="hint">{text.form.requiredNote}</p>
      {failure === null ? null : <ErrorNotice ref={alertRef} message={failureMessage(failure)} />}
      <form
        ref={formRef}
        noValidate
        aria-label={text.form.title}
        onSubmit={(event) => void handleSubmit(event)}
      >
        <div className="form-grid">
          <Field {...fieldProps('routeCode')} required hint={text.hints.routeCode} />
          <Field
            {...fieldProps('closureDate')}
            required
            type="date"
            max={today}
            hint={text.hints.closureDate}
          />
          <Field {...fieldProps('vehicleCode')} required hint={text.hints.vehicleCode} />
          <Field {...fieldProps('driverCode')} required hint={text.hints.driverCode} />
          <Field {...fieldProps('packagesDelivered')} required inputMode="numeric" />
          <Field {...fieldProps('packagesFailed')} required inputMode="numeric" />
          <Field {...fieldProps('packagesReturned')} required inputMode="numeric" />
          <Field {...fieldProps('kmDriven')} required inputMode="numeric" />
        </div>
        <p className="total">
          <span id="total-packages-label">{text.fields.totalPackages}</span>{' '}
          <output aria-labelledby="total-packages-label">{total ?? text.form.totalUnknown}</output>
        </p>
        <Field
          {...fieldProps('notes')}
          multiline
          optionalLabel={text.form.optional}
          hint={text.hints.notes}
        />
        <button type="submit" disabled={submitting}>
          {submitting ? text.form.submitting : text.form.submit}
        </button>
      </form>
    </section>
  );
}
