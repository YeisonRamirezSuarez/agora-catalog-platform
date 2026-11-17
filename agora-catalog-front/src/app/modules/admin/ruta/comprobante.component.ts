import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';

import {apiMessage, AuthService} from '@app/core/services/auth.service';
import {NegocioService} from '@app/core/services/negocio.service';
import {PanelShellComponent} from '@app/shared/layouts/panel-shell/panel-shell.component';
import {ADMIN_LINKS, cop} from '../admin.service';
import {abrevUnidad, cantidad, fechaHora, ItemPedido, numeroPedido, Pedido, RutaService} from './ruta.service';

/**
 * El comprobante de un pedido a una tienda: para mostrarlo en el celular, mandarlo por WhatsApp o
 * imprimirlo. Es un comprobante de entrega, no una factura.
 */
@Component({
  selector: 'app-comprobante',
  imports: [PanelShellComponent, RouterLink],
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
          <a class="btn btn-dark col-span-2 bg-whatsapp hover:bg-whatsapp-press" [href]="whatsapp()" target="_blank" rel="noopener">
            Enviar por WhatsApp
          </a>
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
            @if (p.tiendaDireccion) { <p><span class="text-muted">Dirección: </span>{{ p.tiendaDireccion }}</p> }
            @if (p.tiendaTelefono) { <p><span class="text-muted">Teléfono: </span>{{ p.tiendaTelefono }}</p> }
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

  /** El mismo comprobante en texto, listo para WhatsApp (al número de la tienda si lo tiene). */
  readonly whatsapp = computed(() => {
    const p = this.pedido();
    if (!p) return '';
    const lineas = p.items.map(i => `• ${cantidad(i.cantidad)} ${this.abrev(i)} ${i.productoNombre}`
      + (i.presentacion ? ` (${i.presentacion})` : '') + ` — ${cop(i.subtotal)}`);
    const texto = [
      `*${this.negocio.negocio()?.nombre ?? this.empresa()}*`,
      `Comprobante de entrega ${numeroPedido(p.numero)}` + (p.editadoAt ? ' (corregido)' : ''),
      `${p.tiendaNombre} · ${fechaHora(p.createdAt)}`,
      '',
      ...lineas,
      '',
      `*Total: ${cop(p.total)}*`,
      ...(p.notas ? ['', `Notas: ${p.notas}`] : [])
    ].join('\n');
    const tel = (p.tiendaTelefono ?? '').replace(/\D/g, '');
    // Celular colombiano de 10 dígitos: se le antepone el 57.
    const numero = tel.length === 10 && tel.startsWith('3') ? '57' + tel : tel;
    return `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;
  });

  ngOnInit(): void {
    this.nuevo.set(!!this.route.snapshot.queryParamMap.get('nuevo'));
    this.editado.set(!!this.route.snapshot.queryParamMap.get('editado'));
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
