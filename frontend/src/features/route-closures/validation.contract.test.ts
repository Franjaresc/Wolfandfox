import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { expect, test } from 'vitest';
import { parse } from 'yaml';
import { formFields, limits } from './validation';

interface Schema {
  type?: string;
  pattern?: string;
  maxLength?: number;
  minimum?: number;
  maximum?: number;
  format?: string;
  default?: number;
  required?: string[];
  properties?: Record<string, Schema>;
}

interface Contract {
  components: { schemas: Record<string, Schema> };
  paths: Record<string, { get?: { parameters?: { name: string; schema: Schema }[] } } | undefined>;
}

const contract = parse(
  readFileSync(resolve(process.cwd(), '../contracts/openapi.yaml'), 'utf8'),
) as Contract;
const request = contract.components.schemas.CloseRouteRequest;
const code = contract.components.schemas.Code;

function property(name: string): Schema {
  const schema = request?.properties?.[name];
  if (schema === undefined) {
    throw new Error(`The contract has no property ${name}`);
  }
  return schema;
}

function listParameter(name: string): Schema {
  const parameter = contract.paths['/api/v1/route-closures']?.get?.parameters?.find(
    (candidate) => candidate.name === name,
  );
  if (parameter === undefined) {
    throw new Error(`The contract has no list parameter ${name}`);
  }
  return parameter.schema;
}

test('code rules match the contract', () => {
  expect(limits.codePattern).toBe(code?.pattern);
  expect(limits.codeMaxLength).toBe(code?.maxLength);
});

test.each(['packagesDelivered', 'packagesFailed', 'packagesReturned', 'kmDriven'])(
  '%s has the type and limits the contract declares',
  (name) => {
    const schema = property(name);

    // Whole numbers only. If the contract ever allows decimals, the form's rule must change too.
    expect(schema.type).toBe('integer');
    expect(limits.integerMinimum).toBe(schema.minimum);
    expect(schema.format).toBe('int32');
    expect(limits.integerMaximum).toBe(2 ** 31 - 1);
  },
);

test('notes limit matches the contract', () => {
  expect(limits.notesMaxLength).toBe(property('notes').maxLength);
});

test('page size limits match the contract', () => {
  expect(limits.pageSizeDefault).toBe(listParameter('size').default);
  expect(limits.pageSizeMaximum).toBe(listParameter('size').maximum);
});

test('the form has exactly the fields of the contract', () => {
  expect([...formFields].sort()).toEqual(Object.keys(request?.properties ?? {}).sort());
});

test('only notes is optional, as in the contract', () => {
  const optional = formFields.filter((field) => !request?.required?.includes(field));

  expect(optional).toEqual(['notes']);
});
