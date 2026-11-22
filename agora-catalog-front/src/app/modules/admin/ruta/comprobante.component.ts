import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {NegocioService} from '@app/core/services/negocio.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, cop} from '../admin.service';
import {
  abrevUnidad, cantidad, enlaceWhatsapp, fechaHora, ItemPedido, numeroPedido, Pedido, RutaService, telefonoValido, textoComprobante
} from './ruta.service';

/**
 * El comprobante de un pedido a una tienda: para mostrarlo en el celular, mandarlo por WhatsApp o
 * imprimirlo. Es un comprobante de entrega, no una factura.
 */
@Component({
  selector: 'app-comprobante',
  imports: [PanelShellComponent, RouterLink, FormsModule],
  template: `
    <app-panel-shell [subtitle]="empresa()" [links]="links">
      @if (pedido(); as p) {
        @if (nuevo()) {
          <p class="alert-ok mb-6 print:hidden" role="status">
            Pedido {{ numero(p.numero) }} guardado. Se descontó de {{ p.carroNombre }}.
          </p>
        } @else if (editado()) {
          <p class="alert-ok mb-6 print:hidden" role="status">
            Cambios guardados. El carro {{ p.carroNombre }} quedó al día. Si ya habías mandado el comprobante, envíalo de nuevo.
          </p>
        }

        <div class="mb-6 grid grid-cols-2 gap-2 sm:flex print:hidden">
          <button type="button" class="btn btn-dark col-span-2 bg-whatsapp hover:bg-whatsapp-press" (click)="whatsappAbierto.set(true)">
            Enviar por WhatsApp
          </button>
          <button type="button" class="btn" (click)="imprimir()">Imprimir</button>
          @if (!p.carroEliminado && p.estado === 'CONFIRMADO') {
            <a class="btn" [routerLink]="['/admin/pedidos', p.id, 'editar']">Editar pedido</a>
          }
          <a class="btn" [class.col-span-2]="p.carroEliminado" routerLink="/admin/pedidos/nuevo" [queryParams]="p.carroEliminado ? {} : {carro: p.carroId}">Nuevo pedido</a>
        </div>

        <article class="card mx-auto max-w-2xl p-5 sm:p-8 print:max-w-none print:rounded-none print:border-0 print:p-[14mm] print:shadow-none" aria-label="Comprobante">
          <header class="flex items-start justify-between gap-4 border-b border-line pb-4">
            <div>
              <p class="font-display text-2xl font-semibold">{{ negocio.negocio()?.nombre ?? empresa() }}</p>
              <p class="text-sm text-muted">Comprobante de entrega</p>
            </div>
            <div class="shrink-0 text-right">
              <p class="text-lg font-semibold">{{ numero(p.numero) }}</p>
              <p class="text-sm text-muted">{{ fecha(p.createdAt) }}</p>
              @if (p.editadoAt) { <p class="text-xs text-muted">Editado el {{ fecha(p.editadoAt) }}</p> }
            </div>
          </header>

          <section class="grid gap-1 border-b border-line py-4 text-sm">
            <p><span class="text-muted">Tienda: </span><b>{{ p.tiendaNombre }}</b></p>
            <p><span class="text-muted">Entregado desde: </span>{{ p.carroNombre }}@if (p.usuarioNombre) { · {{ p.usuarioNombre }} }</p>
          </section>

          <table class="mt-2 w-full text-sm">
            <thead>
              <tr class="text-left text-xs text-muted">
                <th class="py-2 pr-2 font-semibold">Cant.</th>
                <th class="py-2 pr-2 font-semibold">Producto</th>
                <th class="hidden py-2 pr-2 text-right font-semibold sm:table-cell print:table-cell">V. unit.</th>
                <th class="py-2 text-right font-semibold">Subtotal</th>
              </tr>
            </thead>
            <tbody>
              @for (i of p.items; track $index) {
                <tr class="border-t border-line align-top">
                  <td class="whitespace-nowrap py-2 pr-2 tabular-nums">{{ cant(i.cantidad) }} {{ abrev(i) }}</td>
                  <td class="py-2 pr-2">
                    {{ i.productoNombre }}@if (i.presentacion) { <span class="text-muted">· {{ i.presentacion }}</span> }
                    <span class="block text-xs text-muted sm:hidden print:hidden">{{ cop(i.precioUnitario) }} c/u</span>
                  </td>
                  <td class="hidden py-2 pr-2 text-right tabular-nums sm:table-cell print:table-cell">{{ cop(i.precioUnitario) }}</td>
                  <td class="py-2 text-right tabular-nums">{{ cop(i.subtotal) }}</td>
                </tr>
              }
            </tbody>
          </table>

          <div class="mt-2 flex items-center justify-between border-t-2 border-ink pt-3">
            <span class="text-lg font-semibold">Total</span>
            <span class="text-2xl font-semibold tabular-nums">{{ cop(p.total) }}</span>
          </div>

          @if (p.notas) { <p class="mt-4 rounded-xl bg-wash p-3 text-sm"><b>Notas:</b> {{ p.notas }}</p> }
          <p class="mt-6 text-center text-xs text-muted">Este comprobante no es una factura de venta.</p>
        </article>

        @if (!p.carroEliminado) {
          <p class="mt-6 text-center print:hidden">
            <a class="btn-link text-sm" [routerLink]="['/admin/carros', p.carroId]">Ver lo que queda en {{ p.carroNombre }}</a>
          </p>
        }
      } @else {
        @if (error()) { <p class="alert-error" role="alert">{{ error() }}</p> }
        @else { <p class="text-muted">Cargando…</p> }
      }

      @if (whatsappAbierto() && pedido()) {
        <div class="fixed inset-0 z-50 grid place-items-end bg-black/50 sm:place-items-center sm:p-4 print:hidden" (click)="$event.target === $event.currentTarget ? whatsappAbierto.set(false) : null">
          <div class="card grid w-full max-w-md gap-4 rounded-b-none p-5 sm:rounded-b-2xl sm:p-6" role="dialog" aria-modal="true" aria-label="Enviar por WhatsApp">
            <h2 class="text-xl font-semibold">Enviar por WhatsApp</h2>
            <div class="field">
              <label for="w-tel">Número (opcional)</label>
              <input class="input" id="w-tel" type="tel" inputmode="tel" maxlength="20" autocomplete="off" placeholder="300 123 4567"
                     [ngModel]="telefono()" (ngModelChange)="telefono.set($event)" [class.border-danger]="!telefonoOk()">
              <p class="hint">{{ telefonoOk() ? 'Si lo dejas vacío, WhatsApp te deja escoger a quién mandarlo.' : 'Revisa el número: deben ser 10 dígitos.' }}</p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              @if (telefonoOk()) {
                <a class="btn btn-dark bg-whatsapp hover:bg-whatsapp-press" [href]="whatsapp()" target="_blank" rel="noopener" (click)="whatsappAbierto.set(false)">
                  {{ telefono().trim() ? 'Abrir WhatsApp' : 'Escoger contacto en WhatsApp' }}
                </a>
              } @else {
                <button type="button" class="btn btn-dark" disabled>Abrir WhatsApp</button>
              }
              <button type="button" class="btn-link" (click)="whatsappAbierto.set(false)">Cancelar</button>
            </div>
          </div>
        </div>
      }
    </app-panel-shell>
  `
})
export class ComprobanteComponent implements OnInit {

  private readonly api = inject(RutaService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  readonly negocio = inject(NegocioService);
  readonly links = ADMIN_LINKS;
  readonly cop = cop;
  readonly cant = cantidad;
  readonly fecha = fechaHora;
  readonly numero = numeroPedido;

  readonly empresa = computed(() => this.auth.usuario()?.empresaNombre ?? 'Panel del negocio');
  readonly pedido = signal<Pedido | null>(null);
  readonly error = signal('');
  readonly nuevo = signal(false);
  readonly editado = signal(false);

  /** La ventana para escoger a qué número se manda el comprobante (no se guarda en ningún lado). */
  readonly whatsappAbierto = signal(false);
  readonly telefono = signal('');
  readonly telefonoOk = computed(() => telefonoValido(this.telefono()));

  /** El comprobante en texto, al número escrito o, sin número, al contacto que se escoja en WhatsApp. */
  readonly whatsapp = computed(() => {
    const p = this.pedido();
    return p ? enlaceWhatsapp(textoComprobante(p, this.negocio.negocio()?.nombre ?? this.empresa(), cop), this.telefono()) : '';
  });

  ngOnInit(): void {
    this.nuevo.set(!!this.route.snapshot.queryParamMap.get('nuevo'));
    this.editado.set(!!this.route.snapshot.queryParamMap.get('editado'));
    // Desde «Nuevo pedido» con WhatsApp marcado, cuando el navegador no dejó abrir la pestaña sola.
    if (this.route.snapshot.queryParamMap.get('whatsapp')) {
      this.telefono.set(this.route.snapshot.queryParamMap.get('tel') ?? '');
      this.whatsappAbierto.set(true);
    }
    this.api.pedido(Number(this.route.snapshot.paramMap.get('id'))).subscribe({
      next: p => this.pedido.set(p),
      error: err => this.error.set(apiMessage(err, 'No encontramos este pedido.'))
    });
  }

  abrev(i: ItemPedido): string {
    return abrevUnidad(i.unidadVenta, i.presentacion);
  }

  imprimir(): void {
    window.print();
  }
}
