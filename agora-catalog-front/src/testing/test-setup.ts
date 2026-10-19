/**
 * Se ejecuta una vez antes de todos los `.spec.ts` (`setupFiles` del target `test` en
 * `angular.json`).
 *
 * No inicializa el TestBed: el builder `@angular/build:unit-test` ya lo hace, y llamar a
 * `initTestEnvironment` acá falla con "Cannot set base providers because it has already been
 * called". Solo tapa huecos de jsdom, que no implementa el DOM completo.
 */

if (!Element.prototype.scrollTo) {
  Element.prototype.scrollTo = () => undefined;
}
if (!window.scrollTo) {
  window.scrollTo = () => undefined;
}

/**
 * Node 25 expone `localStorage` y `sessionStorage` como globales sin `getItem` (sin
 * `--localstorage-file` vienen vacíos), y esos le ganan a los de jsdom. El pedido y los favoritos
 * del catálogo viven en `localStorage`, así que se rellena con uno en memoria solo si falta
 * `getItem`: el día que el entorno lo traiga completo, gana el real.
 */
function createMemoryStorage(): Storage {
  const data = new Map<string, string>();
  return {
    get length(): number {
      return data.size;
    },
    key: (index: number): string | null => Array.from(data.keys())[index] ?? null,
    getItem: (key: string): string | null => (data.has(key) ? data.get(key)! : null),
    setItem: (key: string, value: string): void => {
      data.set(String(key), String(value));
    },
    removeItem: (key: string): void => {
      data.delete(key);
    },
    clear: (): void => {
      data.clear();
    },
  } as Storage;
}

for (const name of ['localStorage', 'sessionStorage'] as const) {
  const current = (globalThis as any)[name];
  if (typeof current?.getItem === 'function') {
    continue;
  }
  const storage = createMemoryStorage();
  Object.defineProperty(globalThis, name, {value: storage, writable: true, configurable: true});
  if (typeof window !== 'undefined' && (window as any)[name] !== storage) {
    Object.defineProperty(window, name, {value: storage, writable: true, configurable: true});
  }
}
