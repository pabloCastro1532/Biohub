import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { AtributoVista } from '../plantillas/plantillas.service';

/**
 * Campos de los atributos de una especie, armados a partir de su plantilla (HU-02).
 * Se usa al registrar y al editar un hallazgo. Emite el conjunto de valores cada vez que el usuario cambia uno.
 */
@Component({
  selector: 'app-atributos-form',
  standalone: true,
  template: `
    <div class="grid gap-4 md:grid-cols-2">
      @for (a of atributos; track a.nombre) {
        <div>
          <label [for]="prefijo + '-' + a.nombre" class="etiqueta">{{ a.nombre }}{{ a.requerido ? ' *' : '' }}</label>
          @switch (a.tipo) {
            @case ('ENUMERADO') {
              <select [id]="prefijo + '-' + a.nombre" (change)="poner(a, $event)" class="campo">
                <option value="">Sin elegir</option>
                @for (v of a.valores; track v) {
                  <option [value]="v" [selected]="valores[a.nombre] === v">{{ v }}</option>
                }
              </select>
            }
            @case ('BOOLEANO') {
              <input
                [id]="prefijo + '-' + a.nombre"
                type="checkbox"
                [checked]="valores[a.nombre] === true"
                (change)="poner(a, $event)"
                class="h-5 w-5 rounded border-slate-300 accent-bio-600"
              />
            }
            @case ('NUMERO') {
              <input
                [id]="prefijo + '-' + a.nombre"
                type="number"
                step="any"
                [value]="valores[a.nombre] ?? ''"
                (input)="poner(a, $event)"
                class="campo"
              />
            }
            @case ('FECHA') {
              <input
                [id]="prefijo + '-' + a.nombre"
                type="date"
                [value]="valores[a.nombre] ?? ''"
                (input)="poner(a, $event)"
                class="campo"
              />
            }
            @default {
              <input
                [id]="prefijo + '-' + a.nombre"
                type="text"
                [value]="valores[a.nombre] ?? ''"
                (input)="poner(a, $event)"
                class="campo"
              />
            }
          }
        </div>
      }
    </div>
  `
})
export class AtributosFormComponent implements OnChanges {
  @Input({ required: true }) atributos: AtributoVista[] = [];
  /** Valores con los que arranca el formulario (por ejemplo, los de la versión actual al editar). */
  @Input() valores: Record<string, unknown> = {};
  /** Evita ids repetidos si hay dos formularios en la misma pantalla. */
  @Input() prefijo = 'attr';
  @Output() valoresChange = new EventEmitter<Record<string, unknown>>();

  private actual: Record<string, unknown> = {};

  ngOnChanges(cambios: SimpleChanges): void {
    if (cambios['valores']) {
      this.actual = { ...this.valores };
    }
  }

  /** Lee el campo según su tipo; un campo vacío se quita para que "obligatorio" falle como corresponde. */
  poner(a: AtributoVista, ev: Event): void {
    const campo = ev.target as HTMLInputElement;
    const siguiente = { ...this.actual };

    if (a.tipo === 'BOOLEANO') {
      siguiente[a.nombre] = campo.checked;
    } else if (campo.value === '') {
      delete siguiente[a.nombre];
    } else {
      siguiente[a.nombre] = a.tipo === 'NUMERO' ? Number(campo.value) : campo.value;
    }
    this.actual = siguiente;
    this.valoresChange.emit({ ...siguiente });
  }
}
