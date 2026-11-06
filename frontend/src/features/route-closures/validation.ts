import type { components } from '../../api/schema.gen';

export type CloseRouteRequest = components['schemas']['CloseRouteRequest'];
export type RouteClosure = components['schemas']['RouteClosure'];

/** Generated types carry no patterns or limits. validation.contract.test.ts keeps these in sync. */
export const limits = {
  codePattern: '^[A-Z0-9][A-Z0-9-]{1,19}$',
  codeMaxLength: 20,
  integerMinimum: 0,
  integerMaximum: 2147483647,
  notesMaxLength: 1000,
  pageSizeDefault: 20,
  pageSizeMaximum: 100,
} as const;

export const formFields = [
  'routeCode',
  'closureDate',
  'vehicleCode',
  'driverCode',
  'packagesDelivered',
  'packagesFailed',
  'packagesReturned',
  'kmDriven',
  'notes',
] as const satisfies readonly (keyof CloseRouteRequest)[];

export type FormField = (typeof formFields)[number];
export type FormValues = Record<FormField, string>;
export type ValidationError = 'required' | 'format' | 'integer' | 'tooLarge' | 'tooLong';
/** 'rejected' means the server refused the field after it passed the checks here. */
export type FieldError = ValidationError | 'rejected';
export type FormErrors = Partial<Record<FormField, FieldError>>;

const CODE = new RegExp(limits.codePattern);
const DIGITS = /^\d+$/;

export function isValidCode(value: string): boolean {
  return CODE.test(value);
}

export function isValidDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }
  const date = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().startsWith(value);
}

function validateCode(value: string): ValidationError | undefined {
  if (value === '') {
    return 'required';
  }
  return isValidCode(value) ? undefined : 'format';
}

function validateDate(value: string): ValidationError | undefined {
  if (value === '') {
    return 'required';
  }
  return isValidDate(value) ? undefined : 'format';
}

function validateInteger(value: string): ValidationError | undefined {
  if (value === '') {
    return 'required';
  }
  if (!DIGITS.test(value)) {
    return 'integer';
  }
  return Number(value) > limits.integerMaximum ? 'tooLarge' : undefined;
}

export function validateField(field: FormField, value: string): ValidationError | undefined {
  switch (field) {
    case 'routeCode':
    case 'vehicleCode':
    case 'driverCode':
      return validateCode(value);
    case 'closureDate':
      return validateDate(value);
    case 'packagesDelivered':
    case 'packagesFailed':
    case 'packagesReturned':
    case 'kmDriven':
      return validateInteger(value);
    case 'notes':
      return value.length > limits.notesMaxLength ? 'tooLong' : undefined;
  }
}

export function validateForm(values: FormValues): FormErrors {
  const errors: FormErrors = {};
  for (const field of formFields) {
    const error = validateField(field, values[field]);
    if (error !== undefined) {
      errors[field] = error;
    }
  }
  return errors;
}

/** The live total: blank counts as 0, and null means a count cannot be added up yet. */
export function liveTotal(values: FormValues): number | null {
  const counts = [values.packagesDelivered, values.packagesFailed, values.packagesReturned];
  if (counts.some((count) => count !== '' && validateInteger(count) !== undefined)) {
    return null;
  }
  return counts.reduce((total, count) => total + Number(count), 0);
}

export function toRequest(values: FormValues): CloseRouteRequest {
  return {
    routeCode: values.routeCode,
    closureDate: values.closureDate,
    vehicleCode: values.vehicleCode,
    driverCode: values.driverCode,
    packagesDelivered: Number(values.packagesDelivered),
    packagesFailed: Number(values.packagesFailed),
    packagesReturned: Number(values.packagesReturned),
    kmDriven: Number(values.kmDriven),
    ...(values.notes.trim() === '' ? {} : { notes: values.notes }),
  };
}

export function isFormField(name: string): name is FormField {
  return (formFields as readonly string[]).includes(name);
}
