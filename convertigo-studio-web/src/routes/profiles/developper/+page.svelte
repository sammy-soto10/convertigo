<script>
	// Supports weights 100-800
	import '@fontsource-variable/inter';
	import {
		AppRail,
		AppRailAnchor,
		ProgressRadial,
		LightSwitch,
		AppBar,
		AppShell,
		TabGroup,
		Tab
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { linear } from 'svelte/easing';
	import { call } from '$lib/utils/service';
	import { modeCurrent } from '@skeletonlabs/skeleton';
	import { localStorageStore } from '@skeletonlabs/skeleton';

	// @ts-ignore
	import IconCloud from '~icons/mdi/cloud-outline';
	// @ts-ignore
	import IconFileTree from '~icons/mdi/file-tree';
	// @ts-ignore
	import IconEditor from '~icons/mdi/application-edit-outline';
	// @ts-ignore
	import IconProperties from '~icons/mdi/list-box';
	// @ts-ignore
	import IconPalette from '~icons/mdi/palette-outline';
	// @ts-ignore
	import IconLogout from '~icons/mdi/logout';
	// @ts-ignore
	import IconMenu from '~icons/mdi/menu';

	import Monaco from '$lib/editor/Editor.svelte';
	import C8oTree from '$lib/treeview/Treeview.svelte';
	import { properties } from '$lib/properties/propertiesStore';
	import { categories } from '$lib/palette/paletteStore';
	import Palette from '$lib/palette/Palette.svelte';

	let currentTile = 0;

	let treeWidth = localStorageStore('studio.treeWidth', 100);
	let propertiesWidth = localStorageStore('studio.propertiesWidth', 100);
	let paletteWidth = localStorageStore('studio.paletteWidth', 100);
	let editorTab = 0;
	let treeSelected = localStorageStore('studio.treeSelected', false);
	let propertiesSelected = localStorageStore('studio.propertiesSelected', false);
	let paletteSelected = localStorageStore('studio.paletteSelected', false);
	let editorSelected = localStorageStore('studio.editorSelected', false);
	let authenticated = false;

	/**
	 * @type {HTMLImageElement}
	 */
	let img;
	onMount(() => {
		document.body.setAttribute('data-theme', 'developper-theme');
		call('engine.CheckAuthentication').then((res) => {
			authenticated = res.admin.authenticated;
			if (!authenticated) {
				if (!location.href.includes('/studio')) {
					sessionStorage.setItem('studioWebDev', 'true');
					location.href = '/convertigo/admin/login.html';
				} else {
					sessionStorage.setItem('studioWebDev', 'false');
					location.href = location.href.replace(/\/studio\/.*/, '/admin/login.html');
				}
			}
		});
	});

	function treeWidthDrag(e) {
		if (e.layerX > 0) {
			$treeWidth = e.x - e.target.parentElement.offsetLeft;
		}
	}

	function propertiesWidthDrag(e) {
		if (e.layerX > 0) {
			$propertiesWidth = e.x - e.target.parentElement.offsetLeft;
		}
	}

	function paletteWidthDrag(e) {
		if (!isPaletteDragItem(e)) {
			if (e.layerX > 0) {
				$paletteWidth = e.x - e.target.parentElement.offsetLeft;
			}
		}
	}

	/**
	 * @param {HTMLDivElement} node
	 * @param {any} duration
	 */
	function withTransition(node, { duration }) {
		return {
			duration,
			css: (t) => {
				let l = Math.round(linear(t) * node.clientWidth);
				return `
					width: ${l}px;
					min-width: ${l}px;
					opacity: ${t};
				`;
			}
		};
	}

	// @ts-ignore
	function noDragImage(e) {
		if (!isPaletteDragItem(e)) {
			e.target.parentElement.parentElement.classList.remove('widthTransition');
			e.dataTransfer.setDragImage(img, 0, 0);
		}
	}

	function isPaletteDragItem(e) {
		return e.target.classList.contains('palette-item');
	}

	let treeNodes = [
		{
			content: 'Please update'
		}
	];

	async function update() {
		let json = await call('engine.Authenticate', {
			authType: 'login',
			authUserName: 'admin',
			authPassword: 'admin'
		});
		json = await call('studio.treeview.Get');
		console.log('json: ' + JSON.stringify(json));
		treeNodes = json.children.map((p) => {
			return {
				content: p.label,
				children: p.children
			};
		});
	}

	async function handleTreeClicked(e) {
		let id = e.detail.id;

		// update properties store
		let treeData = await call('studio.properties.Get', { id });
		properties.set(treeData.properties);

		// update palette store
		let paletteData = await call('studio.palette.Get', { id });
		categories.set(paletteData.categories);
	}
</script>

<AppShell>
	<svelte:fragment slot="header"
		><AppBar
			gridColumns="grid-cols-3"
			slotDefault="place-self-center"
			slotTrail="place-content-end"
		>
			<svelte:fragment slot="lead">
				<IconMenu style="margin-left:8.805px" />
			</svelte:fragment>
			<h1 style="font-weight: 800">Convertigo Low Code Studio</h1>
			<svelte:fragment slot="trail">
				<LightSwitch /></svelte:fragment
			>
		</AppBar></svelte:fragment
	>
	<svelte:fragment slot="sidebarLeft"
		><AppRail background="bg-surface-700" width="w-14">
			<svelte:fragment slot="lead">
				<AppRailAnchor selected={$treeSelected} on:click={() => ($treeSelected = !$treeSelected)}
					><IconFileTree /></AppRailAnchor
				>
				<AppRailAnchor
					selected={$propertiesSelected}
					on:click={() => ($propertiesSelected = !$propertiesSelected)}
					><IconProperties /></AppRailAnchor
				>
				<AppRailAnchor
					selected={$paletteSelected}
					on:click={() => ($paletteSelected = !$paletteSelected)}><IconPalette /></AppRailAnchor
				>
				<AppRailAnchor
					selected={$editorSelected}
					on:click={() => ($editorSelected = !$editorSelected)}><IconEditor /></AppRailAnchor
				>
				<!--<AppRailAnchor
					selected={propertiesSelected}
					on:click={async () => {
						console.log(await callService('projects.List'));
					}}><IconProperties /></AppRailAnchor
				>-->
			</svelte:fragment>
			<svelte:fragment slot="trail">
				<AppRailAnchor rel="external" href="/convertigo/admin/" title="Admin"
					><IconLogout /></AppRailAnchor
				>
			</svelte:fragment>
		</AppRail></svelte:fragment
	>
	<!-- (sidebarRight) -->
	<!-- (pageHeader) -->
	<!-- Router Slot -->
	<div class="flex flex-row items-stretch h-full">
		{#if $treeSelected}
			<!-- svelte-ignore a11y-no-static-element-interactions -->
			<div
				class="card m-1 variant-soft-primary overflow-hidden widthTransition"
				style:width="{$treeWidth}px"
				style:min-width="100px"
				on:drag={treeWidthDrag}
				on:dragstart={noDragImage}
				transition:withTransition={{ duration: 250 }}
			>
				<div class="flex flex-row items-stretch h-full">
					<div
						class="flex-col flex items-stretch grow scroll-smooth overflow-y-auto snap-y scroll-px-4 snap-mandatory"
					>
						{#if authenticated}
							<C8oTree on:treeClick={handleTreeClicked} />
						{:else}
							<ProgressRadial
								...
								stroke={100}
								meter="stroke-primary-500"
								track="stroke-primary-500/30"
							/>
						{/if}
					</div>
					<span class="draggable divider-vertical h-full border-2" draggable="true" />
				</div>
			</div>
		{/if}
		{#if $propertiesSelected}
			<!-- svelte-ignore a11y-no-static-element-interactions -->
			<div
				class="card m-1 variant-soft-primary overflow-hidden widthTransition"
				style:width="{$propertiesWidth}px"
				style:min-width="100px"
				on:drag={propertiesWidthDrag}
				on:dragstart={noDragImage}
				transition:withTransition={{ duration: 250 }}
			>
				<div class="flex flex-row items-stretch h-full">
					<!-- Responsive Container (recommended) -->
					<div class="table-container">
						<!-- Native Table Element -->
						<table class="table table-hover table-compact">
							<thead>
								<tr>
									<th>Name</th>
									<th>Value</th>
								</tr>
							</thead>
							<tbody>
								{#each Object.entries($properties) as entry}
									<tr>
										<td>{entry[0]}</td>
										<td
											><input
												class="input"
												type="text"
												placeholder={entry[0]}
												value={entry[1]}
											/></td
										>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
					<span class="draggable divider-vertical h-full border-2" draggable="true" />
				</div>
			</div>
		{/if}
		{#if $paletteSelected}
			<!-- svelte-ignore a11y-no-static-element-interactions -->
			<div
				class="card m-1 variant-soft-primary overflow-hidden widthTransition"
				style:width="{$paletteWidth}px"
				style:min-width="100px"
				on:drag={paletteWidthDrag}
				on:dragstart={noDragImage}
				transition:withTransition={{ duration: 250 }}
			>
				<div class="flex flex-row items-stretch h-full">
					<div
						class="flex-col flex items-stretch grow scroll-smooth overflow-y-auto snap-y scroll-px-4 snap-mandatory"
					>
						{#if authenticated}
							<Palette />
						{:else}
							<ProgressRadial
								...
								stroke={100}
								meter="stroke-primary-500"
								track="stroke-primary-500/30"
							/>
						{/if}
					</div>
					<span class="draggable divider-vertical h-full border-2" draggable="true" />
				</div>
			</div>
		{/if}
		{#if $editorSelected}
			<div
				class="grow card m-1 variant-soft-success h-full"
				style="height: calc(100% - 8px);"
				transition:withTransition={{ duration: 250 }}
			>
				<TabGroup class="w-full h-full flex flex-col" regionPanel="grow">
					<Tab bind:group={editorTab} name="file1" value={0}>file1.txt</Tab>
					<Tab bind:group={editorTab} name="file2" value={1}>file2.txt</Tab>
					<svelte:fragment slot="panel">
						<div class="w-full h-full grow flex">
							<Monaco
								content="console.log('hello');"
								readOnly={false}
								language="typescript"
								theme={$modeCurrent ? 'vs' : 'vs-dark'}
							/>
						</div>
					</svelte:fragment>
				</TabGroup>
			</div>
		{/if}
	</div>
	<!-- ---- / ---- -->
	<!-- (pageFooter) -->
	<!-- (footer) -->
</AppShell>

<style>
	.draggable {
		cursor: grab;
	}
	.card:active {
		cursor: grabbing;
	}
	.widthTransition {
		transition-property: min-width;
		transition-duration: 250ms;
	}

	:global(#page-content) {
		overflow-y: hidden;
	}

	:global(.input) {
		padding: 0px 10px;
	}
	.apprailColor {
		/*background-color: #3A3D41;*/
		background-color: red;
	}
</style>
