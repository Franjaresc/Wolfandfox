import { useId, type ChangeEvent, type HTMLInputTypeAttribute } from 'react';

interface FieldProps {
  name: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string | undefined;
  hint?: string | undefined;
  required?: boolean;
  optionalLabel?: string | undefined;
  type?: HTMLInputTypeAttribute;
  inputMode?: 'numeric' | 'text';
  multiline?: boolean;
}

/** A labelled input whose hint and error are announced by assistive technology. */
export function Field({
  name,
  label,
  value,
  onChange,
  error,
  hint,
  required = false,
  optionalLabel,
  type = 'text',
  inputMode,
  multiline = false,
}: FieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;
  const describedBy =
    [hint === undefined ? null : hintId, error === undefined ? null : errorId]
      .filter((describer) => describer !== null)
      .join(' ') || undefined;
  const shared = {
    id,
    name,
    value,
    required,
    'aria-invalid': error === undefined ? undefined : true,
    'aria-describedby': describedBy,
    onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      onChange(event.target.value);
    },
  };

  return (
    <div className="field">
      <label htmlFor={id}>
        {label}
        {optionalLabel === undefined ? null : <span className="muted"> {optionalLabel}</span>}
      </label>
      {hint === undefined ? null : (
        <p id={hintId} className="hint">
          {hint}
        </p>
      )}
      {multiline ? (
        <textarea rows={3} {...shared} />
      ) : (
        <input type={type} inputMode={inputMode} autoComplete="off" {...shared} />
      )}
      {error === undefined ? null : (
        <p id={errorId} className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}
