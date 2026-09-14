import { promises as fs } from "fs";
import path from "path";
import { randomUUID } from "crypto";
import type { Dish, CreateDishInput, UpdateDishInput } from "@/lib/models/dish";
import type {
  Production,
  CreateProductionInput,
  UpdateProductionInput,
} from "@/lib/models/production";

/**
 * Backend de referência baseado em arquivo JSON local.
 *
 * Isto existe para o projeto funcionar de ponta a ponta sem depender de uma
 * infraestrutura externa. Para produção, troque as duas classes abaixo por
 * implementações sobre um banco real (Postgres via Prisma, por exemplo),
 * mantendo a mesma interface pública — nenhum código de rota ou componente
 * precisa mudar.
 *
 * Segurança: todo acesso é local ao processo do servidor; nada aqui é
 * exposto ao cliente. IDs são gerados com randomUUID (não sequenciais,
 * não adivinháveis).
 */

const DATA_DIR = path.join(process.cwd(), "data");
const DISHES_FILE = path.join(DATA_DIR, "dishes.json");
const PRODUCTIONS_FILE = path.join(DATA_DIR, "productions.json");

async function ensureDataFile(file: string): Promise<void> {
  await fs.mkdir(DATA_DIR, { recursive: true });
  try {
    await fs.access(file);
  } catch {
    await fs.writeFile(file, "[]", "utf8");
  }
}

async function readJsonArray<T>(file: string): Promise<T[]> {
  await ensureDataFile(file);
  const raw = await fs.readFile(file, "utf8");
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as T[]) : [];
  } catch {
    // Arquivo corrompido — falha de forma segura para uma lista vazia em vez de derrubar a API.
    return [];
  }
}

// Serializa escritas por arquivo para evitar corrida entre requisições concorrentes
// (limitação conhecida de um backend baseado em arquivo — um banco real resolve isso nativamente).
const writeLocks = new Map<string, Promise<void>>();
async function writeJsonArray<T>(file: string, data: T[]): Promise<void> {
  const prior = writeLocks.get(file) ?? Promise.resolve();
  const next = prior
    .catch(() => {})
    .then(async () => {
      const tmp = `${file}.${randomUUID()}.tmp`;
      await fs.writeFile(tmp, JSON.stringify(data, null, 2), "utf8");
      await fs.rename(tmp, file); // rename é atômico no mesmo filesystem
    });
  writeLocks.set(file, next);
  return next;
}

function nowIso(): string {
  return new Date().toISOString();
}

/* ---------------------------- Dishes repository ---------------------------- */

export const dishRepository = {
  async list(): Promise<Dish[]> {
    return readJsonArray<Dish>(DISHES_FILE);
  },

  async getById(id: string): Promise<Dish | null> {
    const all = await this.list();
    return all.find((d) => d.id === id) ?? null;
  },

  async create(input: CreateDishInput): Promise<Dish> {
    const all = await this.list();
    const timestamp = nowIso();
    const dish: Dish = {
      ...input,
      id: randomUUID(),
      createdAt: timestamp,
      updatedAt: timestamp,
    };
    all.push(dish);
    await writeJsonArray(DISHES_FILE, all);
    return dish;
  },

  async update(id: string, input: UpdateDishInput): Promise<Dish | null> {
    const all = await this.list();
    const idx = all.findIndex((d) => d.id === id);
    if (idx === -1) return null;
    const existing = all[idx]!;
    const updated: Dish = { ...existing, ...input, id: existing.id, updatedAt: nowIso() };
    all[idx] = updated;
    await writeJsonArray(DISHES_FILE, all);
    return updated;
  },

  async remove(id: string): Promise<boolean> {
    const all = await this.list();
    const next = all.filter((d) => d.id !== id);
    if (next.length === all.length) return false;
    await writeJsonArray(DISHES_FILE, next);
    return true;
  },

  async seedIfEmpty(seed: Dish[]): Promise<void> {
    const all = await this.list();
    if (all.length === 0) {
      await writeJsonArray(DISHES_FILE, seed);
    }
  },
};

/* -------------------------- Productions repository -------------------------- */

export const productionRepository = {
  async list(): Promise<Production[]> {
    return readJsonArray<Production>(PRODUCTIONS_FILE);
  },

  async getById(id: string): Promise<Production | null> {
    const all = await this.list();
    return all.find((p) => p.id === id) ?? null;
  },

  async create(input: CreateProductionInput): Promise<Production> {
    const all = await this.list();
    const timestamp = nowIso();
    const production: Production = {
      ...input,
      id: randomUUID(),
      createdAt: timestamp,
      updatedAt: timestamp,
    };
    all.push(production);
    await writeJsonArray(PRODUCTIONS_FILE, all);
    return production;
  },

  async update(id: string, input: UpdateProductionInput): Promise<Production | null> {
    const all = await this.list();
    const idx = all.findIndex((p) => p.id === id);
    if (idx === -1) return null;
    const existing = all[idx]!;
    const updated: Production = { ...existing, ...input, id: existing.id, updatedAt: nowIso() };
    all[idx] = updated;
    await writeJsonArray(PRODUCTIONS_FILE, all);
    return updated;
  },

  async remove(id: string): Promise<boolean> {
    const all = await this.list();
    const next = all.filter((p) => p.id !== id);
    if (next.length === all.length) return false;
    await writeJsonArray(PRODUCTIONS_FILE, next);
    return true;
  },
};
