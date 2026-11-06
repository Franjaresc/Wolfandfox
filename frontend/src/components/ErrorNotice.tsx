import type { Ref } from 'react';
import { messages } from '../messages';

interface ErrorNoticeProps {
  message: string;
  onRetry?: (() => void) | undefined;
  ref?: Ref<HTMLDivElement>;
}

export function ErrorNotice({ message, onRetry, ref }: ErrorNoticeProps) {
  return (
    <div ref={ref} role="alert" tabIndex={-1} className="notice notice-error">
      <p>{message}</p>
      {onRetry === undefined ? null : (
        <button type="button" onClick={onRetry}>
          {messages.common.retry}
        </button>
      )}
    </div>
  );
}
